package dukku.logconsumer.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dukku.logconsumer.document.AnomalyLogDoc;
import dukku.logconsumer.repository.AnomalyLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * 이상거래 탐지 이벤트 Kafka Consumer
 * - settlement.anomaly.detected 토픽에서 이벤트 수신
 * - MongoDB anomaly_logs 컬렉션에 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnomalyKafkaConsumer {

    private final AnomalyLogRepository anomalyLogRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "settlement.anomaly.detected", groupId = "anomaly-consumer-group")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode node = objectMapper.readTree(record.value());

            AnomalyLogDoc doc = AnomalyLogDoc.builder()
                    .settlementUuid(textOrNull(node, "settlementUuid"))
                    .sellerUuid(textOrNull(node, "sellerUuid"))
                    .orderId(textOrNull(node, "orderId"))
                    .anomalyType(textOrNull(node, "anomalyType"))
                    .severity(textOrNull(node, "severity"))
                    .description(textOrNull(node, "description"))
                    .expectedValue(longOrNull(node, "expectedValue"))
                    .actualValue(longOrNull(node, "actualValue"))
                    .detectedAt(parseDetectedAt(node))
                    .build();

            anomalyLogRepository.save(doc);
            log.info("이상거래 로그 저장 완료 - settlementUuid: {}, type: {}, severity: {}",
                    doc.getSettlementUuid(), doc.getAnomalyType(), doc.getSeverity());
        } catch (Exception e) {
            log.error("이상거래 이벤트 파싱/저장 실패 - offset: {}, value: {}", record.offset(), record.value(), e);
        } finally {
            ack.acknowledge();
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asText() : null;
    }

    private Long longOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asLong() : null;
    }

    private Instant parseDetectedAt(JsonNode node) {
        JsonNode tsNode = node.get("detectedAt");
        if (tsNode == null || tsNode.isNull()) {
            return Instant.now();
        }

        try {
            // LocalDateTime 형식으로 전달됨 (e.g. "2025-01-01T12:00:00")
            LocalDateTime ldt = LocalDateTime.parse(tsNode.asText());
            return ldt.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e) {
            try {
                return Instant.parse(tsNode.asText());
            } catch (DateTimeParseException e2) {
                log.warn("탐지 시각 파싱 실패, 현재 시각 사용: {}", tsNode.asText());
                return Instant.now();
            }
        }
    }
}
