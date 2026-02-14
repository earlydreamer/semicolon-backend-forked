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

@Slf4j
@Service
@RequiredArgsConstructor
public class DeductDepositForPaymentUseCase {

    private final DecreaseDepositUseCase decreaseDepositUseCase;
    private final IncreaseDepositUseCase increaseDepositUseCase;
    private final EventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(UUID userUuid, Long totalAmount, UUID orderUuid, UUID paymentUuid,
                        List<PaymentSuccessEvent.ItemDepositUsage> itemDepositUsages) {
        if (totalAmount == null || totalAmount <= 0) {
            return;
        }

        try {
            executeDeductions(userUuid, totalAmount, orderUuid, itemDepositUsages);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            DepositDeductionFailedEvent failEvent = createFailEvent(
                    userUuid, totalAmount, orderUuid, paymentUuid, e);
            publishFailAfterRollback(failEvent);
        }
    }

    private void executeDeductions(UUID userUuid, Long totalAmount, UUID orderUuid,
                                   List<PaymentSuccessEvent.ItemDepositUsage> itemDepositUsages) {
        for (PaymentSuccessEvent.ItemDepositUsage usage : itemDepositUsages) {
            decreaseDepositUseCase.decrease(userUuid, usage.depositAmount(), DepositHistoryType.USE,
                    usage.orderItemUuid());
        }

        increaseDepositUseCase.increase(
                SystemDepositInitData.SYSTEM_USER_UUID,
                totalAmount,
                DepositHistoryType.DEPOSIT_CHARGE,
                orderUuid);

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