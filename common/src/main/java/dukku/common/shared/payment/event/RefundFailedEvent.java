package dukku.common.shared.payment.event;

import dukku.common.global.eventPublisher.KafkaRoutableEvent;
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
        LocalDateTime occurredAt) implements KafkaRoutableEvent {

    @Override
    public String topic() { return TOPIC; }

    /**
     * 이 이벤트가 발행되는 Kafka 토픽명.
     * Producer(EventPublisher)와 Consumer(@KafkaListener)가 동일한 상수를 참조하여
     * 토픽명 불일치를 컴파일 타임에 방지한다.
     */
    public static final String TOPIC = "payment.refund-failed";

    public RefundFailedEvent {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
