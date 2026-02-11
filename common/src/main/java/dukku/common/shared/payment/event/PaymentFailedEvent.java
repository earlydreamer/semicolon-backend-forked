package dukku.common.shared.payment.event;

import dukku.common.global.eventPublisher.KafkaRoutableEvent;
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
public record PaymentFailedEvent(
        UUID orderUuid,
        UUID paymentUuid,
        UUID userUuid,
        PaymentFailureStage failureStage,
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
    public static final String TOPIC = "payment.failed";

    public PaymentFailedEvent(UUID orderUuid, UUID paymentUuid, String reason) {
        this(orderUuid, paymentUuid, null, PaymentFailureStage.SYSTEM, PaymentFailureCode.UNKNOWN, true, reason,
                LocalDateTime.now());
    }

    public PaymentFailedEvent {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
