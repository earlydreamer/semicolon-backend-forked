package dukku.deposit.boundedContext.deposit.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.deposit.event.DepositRefundFailedEvent;
import dukku.common.shared.deposit.event.DepositRefundedEvent;
import dukku.common.shared.deposit.type.DepositFailureCode;
import dukku.common.shared.deposit.type.DepositHistoryType;
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
import java.util.UUID;

/**
 * 환불 처리 UseCase (Saga 참여)
 *
 * <p>
 * 환불 발생 시 예치금을 롤백(재적립)하고 결과를 발행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundDepositUseCase {

    private final IncreaseDepositUseCase increaseDepositUseCase;
    private final DecreaseDepositUseCase decreaseDepositUseCase;
    private final EventPublisher eventPublisher;

    /**
     * 환불 처리 실행
     *
     * @param userUuid    사용자 UUID
     * @param amount      환불 금액
     * @param orderUuid   주문 UUID
     * @param paymentUuid 결제 UUID (실패 이벤트 연계용)
     * @param refundUuid  환불 UUID (Saga 상태 연계용)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(UUID userUuid, Long amount, UUID orderUuid, UUID paymentUuid, UUID refundUuid) {
        if (amount == null || amount <= 0) {
            return;
        }

        try {
            // 환불 금액을 사용자 지갑으로 롤백
            increaseDepositUseCase.increase(userUuid, amount, DepositHistoryType.ROLLBACK, orderUuid);

            // 동일 금액을 시스템 지갑에서 출금
            decreaseDepositUseCase.decrease(
                    SystemDepositInitData.SYSTEM_USER_UUID,
                    amount,
                    DepositHistoryType.ROLLBACK,
                    orderUuid);

            // 롤백 성공 이벤트 발행
            eventPublisher.publish(new DepositRefundedEvent(refundUuid, paymentUuid, orderUuid, userUuid, amount));
        } catch (Exception e) {
            DepositRefundFailedEvent failEvent = new DepositRefundFailedEvent(
                    refundUuid,
                    orderUuid,
                    paymentUuid,
                    userUuid,
                    amount,
                    DepositFailureCode.PERSISTENCE_ERROR,
                    true,
                    buildFailureReason(DepositFailureCode.PERSISTENCE_ERROR, e.getMessage()),
                    LocalDateTime.now());

            // 부분 반영 방지를 위해 트랜잭션 롤백으로 고정
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("[예치금 환불/롤백 실패] userUuid={}, amount={}, orderUuid={}", userUuid, amount, orderUuid, e);

            publishFailAfterRollback(failEvent);
        }
    }

    /**
     * rollback-only 경로에서 실패 이벤트가 유실되지 않도록
     * 롤백 완료 시점에 별도로 발행한다.
     */
    private void publishFailAfterRollback(DepositRefundFailedEvent failEvent) {
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

    private String buildFailureReason(DepositFailureCode code, String detail) {
        if (detail == null || detail.isBlank()) {
            return code.name();
        }
        return code.name() + ": " + detail;
    }
}