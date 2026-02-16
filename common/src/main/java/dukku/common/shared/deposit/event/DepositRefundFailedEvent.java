package dukku.common.shared.deposit.event;

import dukku.common.global.event.DomainEvent;
import dukku.common.shared.deposit.type.DepositFailureCode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 예치금 환불/롤백 실패 이벤트.
 *
 * <p>
 * 환불 후 예치금 롤백이 실패했을 때 발행.
 */
public record DepositRefundFailedEvent(
        UUID refundUuid,
        UUID orderUuid,
        UUID paymentUuid,
        UUID userUuid,
        Long amount,
        DepositFailureCode failureCode,
        boolean retryable,
        String reason,
        LocalDateTime occurredAt) implements DomainEvent {

    public DepositRefundFailedEvent {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

    @Override
    public String getTopic() {
        return "deposit.refund.failed";
    }

    @Override
    public String getKey() {
        return paymentUuid.toString();
    }
}
