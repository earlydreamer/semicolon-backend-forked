package dukku.common.shared.settlement.event;

import dukku.common.global.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 이상거래 탐지 이벤트
 * - 이상거래가 탐지되면 Kafka로 발행
 * - log-consumer에서 수신하여 MongoDB(anomaly_logs)에 저장
 *
 * @param settlementUuid 정산 UUID
 * @param sellerUuid     판매자 UUID
 * @param orderId        주문 UUID
 * @param anomalyType    이상거래 유형
 * @param severity       심각도
 * @param description    상세 설명
 * @param expectedValue  기대값
 * @param actualValue    실제값
 * @param detectedAt     탐지 일시
 */
public record SettlementAnomalyDetectedEvent(
        UUID settlementUuid,
        UUID sellerUuid,
        UUID orderId,
        String anomalyType,
        String severity,
        String description,
        Long expectedValue,
        Long actualValue,
        LocalDateTime detectedAt
) implements DomainEvent {

    @Override
    public String getTopic() {
        return "settlement.anomaly.detected";
    }

    @Override
    public String getKey() {
        return settlementUuid.toString();
    }
}
