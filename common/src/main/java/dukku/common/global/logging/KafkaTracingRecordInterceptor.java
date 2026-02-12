package dukku.common.global.logging;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;

import java.nio.charset.StandardCharsets;


@Slf4j
public class KafkaTracingRecordInterceptor implements RecordInterceptor<String, String> {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";

    /**
     * 리스너 처리 전 호출 - Kafka 헤더에서 traceId/spanId 추출하여 MDC에 세팅
     */
    @Override
    public ConsumerRecord<String, String> intercept(ConsumerRecord<String, String> record,
                                                     Consumer<String, String> consumer) {
        MDC.clear();

        Header traceIdHeader = record.headers().lastHeader(TRACE_ID_HEADER);
        if (traceIdHeader != null) {
            String traceId = new String(traceIdHeader.value(), StandardCharsets.UTF_8);
            MDC.put(MdcLoggingFilter.TRACE_ID, traceId);
            log.debug("MDC set from Kafka header: traceId={}, topic={}, partition={}, offset={}",
                    traceId, record.topic(), record.partition(), record.offset());
        }

        Header spanIdHeader = record.headers().lastHeader(SPAN_ID_HEADER);
        if (spanIdHeader != null) {
            String spanId = new String(spanIdHeader.value(), StandardCharsets.UTF_8);
            MDC.put(MdcLoggingFilter.SPAN_ID, spanId);
        }

        return record;
    }

    /**
     * 리스너 처리 성공 후 호출 - MDC 정리
     */
    @Override
    public void afterRecord(ConsumerRecord<String, String> record,
                            Consumer<String, String> consumer) {
        MDC.clear();
    }
}
