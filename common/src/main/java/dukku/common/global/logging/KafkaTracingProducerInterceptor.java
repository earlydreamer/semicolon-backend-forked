package dukku.common.global.logging;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka Producer Interceptor - MDC의 TraceId를 메시지 헤더에 주입
 */
@Slf4j
public class KafkaTracingProducerInterceptor implements ProducerInterceptor<String, Object> {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        // MDC에서 TraceId 추출하여 Kafka 헤더에 주입
        String traceId = MDC.get(MdcLoggingFilter.TRACE_ID);
        if (traceId != null) {
            record.headers().add(TRACE_ID_HEADER, traceId.getBytes(StandardCharsets.UTF_8));
            log.debug("Injected TraceId into Kafka header: {}", traceId);
        }

        String spanId = MDC.get(MdcLoggingFilter.SPAN_ID);
        if (spanId != null) {
            record.headers().add(SPAN_ID_HEADER, spanId.getBytes(StandardCharsets.UTF_8));
        }

        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // 메시지 전송 결과 처리
        if (exception != null) {
            log.warn("Kafka message send failed: {}", exception.getMessage());
        }
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
