package dukku.common.global.logging;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.listener.BatchInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

@Slf4j
public class KafkaTracingBatchInterceptor implements BatchInterceptor<String, String> {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";

    @Override
    public ConsumerRecords<String, String> intercept(ConsumerRecords<String, String> records,
                                                      Consumer<String, String> consumer) {
        MDC.clear();

        // 배치의 첫 번째 레코드에서 traceId/spanId 추출
        Iterator<ConsumerRecord<String, String>> iterator = records.iterator();
        if (iterator.hasNext()) {
            ConsumerRecord<String, String> first = iterator.next();

            Header traceIdHeader = first.headers().lastHeader(TRACE_ID_HEADER);
            if (traceIdHeader != null) {
                MDC.put(MdcLoggingFilter.TRACE_ID, new String(traceIdHeader.value(), StandardCharsets.UTF_8));
            }

            Header spanIdHeader = first.headers().lastHeader(SPAN_ID_HEADER);
            if (spanIdHeader != null) {
                MDC.put(MdcLoggingFilter.SPAN_ID, new String(spanIdHeader.value(), StandardCharsets.UTF_8));
            }
        }

        log.debug("Kafka batch received: {} records", records.count());
        return records;
    }

    @Override
    public void success(ConsumerRecords<String, String> records, Consumer<String, String> consumer) {
        MDC.clear();
    }

    @Override
    public void failure(ConsumerRecords<String, String> records, Exception exception,
                        Consumer<String, String> consumer) {
        log.error("Kafka batch listener failed: {} records", records.count(), exception);
        MDC.clear();
    }
}
