package dukku.logconsumer.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dukku.logconsumer.document.LogDoc;
import dukku.logconsumer.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogKafkaConsumer {

    private final LogRepository logRepository;
    private final ObjectMapper objectMapper;
    private final LogMetrics logMetrics;

    @KafkaListener(topics = "msa-logs", groupId = "log-consumer-group")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode node = objectMapper.readTree(record.value());

            String traceId = textOrNull(node, "traceId");
            if (traceId == null || traceId.isBlank()) {
                return;
            }

            LogDoc doc = LogDoc.builder()
                    .traceId(traceId)
                    .spanId(textOrNull(node, "spanId"))
                    .userId(textOrNull(node, "userId"))
                    .serviceName(textOrNull(node, "serviceName"))
                    .level(textOrNull(node, "level"))
                    .message(textOrNull(node, "message"))
                    .loggerName(textOrNull(node, "logger_name"))
                    .threadName(textOrNull(node, "thread_name"))
                    .requestUri(textOrNull(node, "requestUri"))
                    .requestMethod(textOrNull(node, "requestMethod"))
                    .clientIp(textOrNull(node, "clientIp"))
                    .timestamp(parseTimestamp(node))
                    .stackTrace(textOrNull(node, "stack_trace"))
                    .build();

            logRepository.save(doc);
            logMetrics.incrementLogCount(doc.getServiceName(), doc.getLevel());
        } catch (Exception e) {
            log.error("로그 메시지 파싱/저장 실패 - offset: {}, value: {}", record.offset(), record.value(), e);
        } finally {
            ack.acknowledge();
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asText() : null;
    }

    private Instant parseTimestamp(JsonNode node) {
        JsonNode tsNode = node.get("@timestamp");
        if (tsNode == null || tsNode.isNull()) {
            return Instant.now();
        }

        try {
            return Instant.parse(tsNode.asText());
        } catch (DateTimeParseException e) {
            log.warn("타임스탬프 파싱 실패, 현재 시각 사용: {}", tsNode.asText());
            return Instant.now();
        }
    }
}
