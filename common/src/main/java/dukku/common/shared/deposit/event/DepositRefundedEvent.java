package dukku.common.shared.deposit.event;

import java.util.UUID;

/**
 * 예치금 환불 성공 이벤트
 *
 * <p>
 * 환불 절차에 따라 예치금이 정상적으로 복구(재적립)되었을 때 발행.
 * 알림 서비스 등에서 사용.
 */
public record DepositRefundedEvent(
        UUID orderUuid,
        UUID userUuid,
        Long amount) {
    /**
     * 이 이벤트가 발행되는 Kafka 토픽명.
     * Producer(EventPublisher)와 Consumer(@KafkaListener)가 동일한 상수를 참조하여
     * 토픽명 불일치를 컴파일 타임에 방지한다.
     */
    public static final String TOPIC = "deposit.refunded";
}
