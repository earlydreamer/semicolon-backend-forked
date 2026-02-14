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

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundDepositUseCase {

    private final IncreaseDepositUseCase increaseDepositUseCase;
    private final DecreaseDepositUseCase decreaseDepositUseCase;
    private final EventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(UUID userUuid, Long amount, UUID orderUuid, UUID paymentUuid, UUID refundUuid) {
        if (amount == null || amount <= 0) {
            return;
        }

        try {
            increaseDepositUseCase.increase(userUuid, amount, DepositHistoryType.ROLLBACK, orderUuid);
            decreaseDepositUseCase.decrease(
                    SystemDepositInitData.SYSTEM_USER_UUID,
                    amount,
                    DepositHistoryType.ROLLBACK,
                    orderUuid);

            eventPublisher.publish(new DepositRefundedEvent(refundUuid, paymentUuid, orderUuid, userUuid, amount));
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

            log.error("[예치금 환불/롤백 실패] userUuid={}, amount={}, orderUuid={}", userUuid, amount, orderUuid, e);
            eventPublisher.publish(new DepositRefundFailedEvent(
                    refundUuid,
                    orderUuid,
                    paymentUuid,
                    userUuid,
                    amount,
                    DepositFailureCode.PERSISTENCE_ERROR,
                    true,
                    buildFailureReason(DepositFailureCode.PERSISTENCE_ERROR, e.getMessage()),
                    LocalDateTime.now()));
        }
    }

    private String buildFailureReason(DepositFailureCode code, String detail) {
        if (detail == null || detail.isBlank()) {
            return code.name();
        }
        return code.name() + ": " + detail;
    }
}