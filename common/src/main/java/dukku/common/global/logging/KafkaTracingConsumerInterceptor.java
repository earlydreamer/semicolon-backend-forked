package dukku.common.global.logging;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka Consumer Interceptor - 메시지 헤더에서 TraceId 추출
 */
@Slf4j
public class KafkaTracingConsumerInterceptor implements ConsumerInterceptor<String, Object> {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";

    @Override
    public ConsumerRecords<String, Object> onConsume(ConsumerRecords<String, Object> records) {
        records.forEach(record -> {
            // Kafka 헤더에서 TraceId 추출
            Header traceIdHeader = record.headers().lastHeader(TRACE_ID_HEADER);
            if (traceIdHeader != null) {
                String traceId = new String(traceIdHeader.value(), StandardCharsets.UTF_8);
                MDC.put(MdcLoggingFilter.TRACE_ID, traceId);
                log.debug("Extracted TraceId from Kafka header: {}", traceId);
            }

            Header spanIdHeader = record.headers().lastHeader(SPAN_ID_HEADER);
            if (spanIdHeader != null) {
                String spanId = new String(spanIdHeader.value(), StandardCharsets.UTF_8);
                MDC.put(MdcLoggingFilter.SPAN_ID, spanId);
            }
        });

        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        // 커밋 후 MDC 정리
        MDC.clear();
    }

    @Override
    public void close() {
        // 리소스 정리
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // 설정
    }
}
