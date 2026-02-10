package dukku.common.shared.settlement.event;

import java.util.UUID;

/**
 * 정산 지급 요청 이벤트
 * - 배치 작업에서 판매자에게 정산금을 지급하기 위해 발행
 * - Deposit BC에서 수신하여 예치금 충전 처리
 */
public record SettlementDepositChargeRequestedEvent(
        UUID userUuid,         // 판매자 UUID
        Long amount,           // 정산 금액
        UUID settlementUuid    // 정산 UUID
) {
    /**
     * 이 이벤트가 발행되는 Kafka 토픽명.
     * Producer(EventPublisher)와 Consumer(@KafkaListener)가 동일한 상수를 참조하여
     * 토픽명 불일치를 컴파일 타임에 방지한다.
     */
    public static final String TOPIC = "settlement.deposit-charge-requested";
}
