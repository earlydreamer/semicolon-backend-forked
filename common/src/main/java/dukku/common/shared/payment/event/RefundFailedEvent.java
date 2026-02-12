package dukku.common.shared.payment.event;

import dukku.common.global.event.DomainEvent;
import dukku.common.shared.payment.type.PaymentFailureCode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 환불 실패 이벤트.
 *
 * <p>
 * 환불 플로우가 완료되기 전에 실패했을 때 발행(PG 취소 오류 등).
 */
public record RefundFailedEvent(
        UUID orderUuid,
        UUID paymentUuid,
        UUID userUuid,
        Long refundAmount,
        Long expectedPgRefundAmount,
        Long expectedDepositRefundAmount,
        PaymentFailureCode failureCode,
        boolean retryable,
        String reason,
        LocalDateTime occurredAt) implements DomainEvent {

    public RefundFailedEvent {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

    @Override
    public String getTopic() {
        return "payment.refund.failed";
    }

    @Override
    public String getKey() {
        return paymentUuid.toString();
    }
}
