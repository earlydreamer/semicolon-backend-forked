package dukku.common.shared.deposit.event;

import java.util.UUID;

// 예치금 충전 실패 이벤트
public record DepositChargeFailedEvent(
                UUID userUuid,
                Long amount,
                UUID settlementUuid,
                String reason) {
    /**
     * 이 이벤트가 발행되는 Kafka 토픽명.
     * Producer(EventPublisher)와 Consumer(@KafkaListener)가 동일한 상수를 참조하여
     * 토픽명 불일치를 컴파일 타임에 방지한다.
     */
    public static final String TOPIC = "deposit.charge-failed";
}
