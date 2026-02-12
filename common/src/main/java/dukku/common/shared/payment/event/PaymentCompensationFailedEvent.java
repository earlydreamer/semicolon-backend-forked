package dukku.common.shared.payment.event;

import dukku.common.global.event.DomainEvent;
import dukku.common.shared.payment.type.PaymentFailureCode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 보상 실패 이벤트.
 *
 * <p>
 * 보상 과정에서 PG 취소 또는 상태 갱신이 실패했을 때 발행.
 */
public record PaymentCompensationFailedEvent(
        UUID orderUuid,
        UUID paymentUuid,
        UUID userUuid,
        PaymentFailureCode failureCode,
        boolean retryable,
        String reason,
        LocalDateTime occurredAt) implements DomainEvent {

    public PaymentCompensationFailedEvent {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

    @Override
    public String getTopic() {
        return "payment.compensation.failed";
    }

    @Override
    public String getKey() {
        return paymentUuid.toString();
    }
}
