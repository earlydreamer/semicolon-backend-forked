package dukku.common.shared.payment.event;

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
        LocalDateTime occurredAt) {

    /**
     * 이 이벤트가 발행되는 Kafka 토픽명.
     * Producer(EventPublisher)와 Consumer(@KafkaListener)가 동일한 상수를 참조하여
     * 토픽명 불일치를 컴파일 타임에 방지한다.
     */
    public static final String TOPIC = "payment.compensation-failed";

    public PaymentCompensationFailedEvent {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
