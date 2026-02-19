package dukku.common.shared.payment.event;

import dukku.common.global.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 환불 요청 이벤트
 *
 * <p>
 * 예치금 환불이 필요한 환불 건에 대해 사가를 시작할 때 발행된다.
 */
public record RefundRequestedEvent(
        UUID refundUuid,
        UUID paymentUuid,
        UUID orderUuid,
        Long refundAmount,
        Long refundDepositAmount,
        UUID userUuid,
        LocalDateTime occurredAt) implements DomainEvent {

    @Override
    public String getTopic() {
        return "payment.refund-requested";
    }

    @Override
    public String getKey() {
        return orderUuid.toString();
    }
}
