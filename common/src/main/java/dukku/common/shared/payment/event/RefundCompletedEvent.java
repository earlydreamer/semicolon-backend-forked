package dukku.common.shared.payment.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 환불 완료 이벤트
 * <p>
 * 환불 처리가 완료되었을 때 발행.
 * 예치금 롤백 및 주문/상품 상태 변경의 트리거가 됩니다.
 */
public record RefundCompletedEvent(
        UUID refundId,
        UUID paymentId,
        UUID orderUuid,
        Long refundAmount,
        Long refundDepositAmount, // 환불된 예치금
        UUID userUuid,
        LocalDateTime occurredAt
) {
    /**
     * 이 이벤트가 발행되는 Kafka 토픽명.
     * Producer(EventPublisher)와 Consumer(@KafkaListener)가 동일한 상수를 참조하여
     * 토픽명 불일치를 컴파일 타임에 방지한다.
     */
    public static final String TOPIC = "payment.refund-completed";
}
