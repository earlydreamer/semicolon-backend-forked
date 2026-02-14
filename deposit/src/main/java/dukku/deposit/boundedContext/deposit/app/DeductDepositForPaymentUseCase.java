package dukku.deposit.boundedContext.deposit.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.deposit.event.DepositDeductionFailedEvent;
import dukku.common.shared.deposit.event.DepositUsedEvent;
import dukku.common.shared.deposit.type.DepositFailureCode;
import dukku.common.shared.deposit.type.DepositHistoryType;
import dukku.common.shared.payment.event.PaymentSuccessEvent;
import dukku.deposit.boundedContext.deposit.exception.NotEnoughDepositException;
import dukku.deposit.global.SystemDepositInitData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 결제에 따른 예치금 차감 UseCase (Saga 패턴 참여)
 *
 * <p>
 * 결제 성공 시 각 상품별로 할당된 예치금을 차감하고,
 * 전체 차감 결과를 이벤트로 전파한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeductDepositForPaymentUseCase {

    private final DecreaseDepositUseCase decreaseDepositUseCase;
    private final IncreaseDepositUseCase increaseDepositUseCase;
    private final EventPublisher eventPublisher;

    /**
     * 결제에 따른 예치금 차감 실행
     *
     * @param userUuid          예치금을 소유한 유저 식별자
     * @param totalAmount       차감될 총 예치금액
     * @param orderUuid         관련 주문 식별자
     * @param paymentUuid       관련 결제 식별자(보상 트리거용)
     * @param itemDepositUsages 상품별 예치금 사용 상세 내역
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(UUID userUuid, Long totalAmount, UUID orderUuid, UUID paymentUuid,
                        List<PaymentSuccessEvent.ItemDepositUsage> itemDepositUsages) {
        if (totalAmount == null || totalAmount <= 0) {
            return;
        }

        try {
            executeDeductions(userUuid, totalAmount, orderUuid, itemDepositUsages);
        } catch (Exception e) {
            // 부분 반영 방지를 위해 트랜잭션을 롤백으로 고정
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

            DepositDeductionFailedEvent failEvent = createFailEvent(
                    userUuid, totalAmount, orderUuid, paymentUuid, e);
            publishFailAfterRollback(failEvent);
        }
    }

    private void executeDeductions(UUID userUuid, Long totalAmount, UUID orderUuid,
                                   List<PaymentSuccessEvent.ItemDepositUsage> itemDepositUsages) {
        // 상품별 예치금 차감 및 이력 생성
        for (PaymentSuccessEvent.ItemDepositUsage usage : itemDepositUsages) {
            decreaseDepositUseCase.decrease(userUuid, usage.depositAmount(), DepositHistoryType.USE,
                    usage.orderItemUuid());
        }

        // 사용자 차감만큼 시스템 지갑으로 입금
        increaseDepositUseCase.increase(
                SystemDepositInitData.SYSTEM_USER_UUID,
                totalAmount,
                DepositHistoryType.DEPOSIT_CHARGE,
                orderUuid);

        // 전체 차감 완료 이벤트 발행
        eventPublisher.publish(new DepositUsedEvent(orderUuid, userUuid, totalAmount));
    }

    private DepositDeductionFailedEvent createFailEvent(
            UUID userUuid, Long amount, UUID orderUuid, UUID paymentUuid, Exception e) {
        String errorMessage = "시스템 오류가 발생했습니다.";
        String logMessage = "[예치금 차감 실패 - 시스템 오류] userUuid={}, amount={}, orderUuid={}";
        DepositFailureCode failureCode = DepositFailureCode.SYSTEM_ERROR;
        boolean retryable = true;

        if (e instanceof NotEnoughDepositException) {
            errorMessage = e.getMessage();
            logMessage = "[예치금 차감 실패 - 잔액 부족] userUuid={}, amount={}, orderUuid={}";
            failureCode = DepositFailureCode.BALANCE_SHORTAGE;
            retryable = false;
            log.warn(logMessage, userUuid, amount, orderUuid);
        } else {
            log.error(logMessage, userUuid, amount, orderUuid, e);
        }

        return new DepositDeductionFailedEvent(
                orderUuid,
                paymentUuid,
                userUuid,
                amount,
                failureCode,
                retryable,
                errorMessage,
                LocalDateTime.now());
    }

    /**
     * rollback-only 경로에서 실패 이벤트가 유실되지 않도록
     * 롤백 완료 시점에 별도로 발행한다.
     */
    private void publishFailAfterRollback(DepositDeductionFailedEvent failEvent) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        eventPublisher.publish(failEvent);
                    }
                }
            });
            return;
        }

        eventPublisher.publish(failEvent);
    }
}