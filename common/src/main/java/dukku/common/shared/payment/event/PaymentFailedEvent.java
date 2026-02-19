package dukku.common.shared.payment.event;

import dukku.common.shared.payment.type.PaymentFailureCode;
import dukku.common.shared.payment.type.PaymentFailureStage;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 실패 이벤트.
 *
 * <p>
 * 결제 플로우 실패 시 발행되며, 다른 BC가 롤백 처리할 수 있도록 알린다.
 */
import dukku.common.global.event.DomainEvent;

public record PaymentFailedEvent(
        UUID orderUuid,
        UUID paymentUuid,
        UUID userUuid,
        PaymentFailureStage failureStage,
        PaymentFailureCode failureCode,
        boolean retryable,
        String reason,
        LocalDateTime occurredAt,
        UUID couponUuid) implements DomainEvent {

    @Override
    public String getTopic() {
        return "payment.failed";
    }

    @Override
    public String getKey() {
        return orderUuid.toString();
    }

    public PaymentFailedEvent(UUID orderUuid, UUID paymentUuid, String reason) {
        this(orderUuid, paymentUuid, null, PaymentFailureStage.SYSTEM, PaymentFailureCode.UNKNOWN, true, reason,
                LocalDateTime.now(), null);
    }

    public PaymentFailedEvent(
            UUID orderUuid,
            UUID paymentUuid,
            UUID userUuid,
            PaymentFailureStage failureStage,
            PaymentFailureCode failureCode,
            boolean retryable,
            String reason,
            LocalDateTime occurredAt) {
        this(orderUuid, paymentUuid, userUuid, failureStage, failureCode, retryable, reason, occurredAt, null);
    }

    public PaymentFailedEvent {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
