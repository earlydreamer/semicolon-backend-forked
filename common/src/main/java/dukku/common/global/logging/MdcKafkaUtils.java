package dukku.common.global.logging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;

public final class MdcKafkaUtils {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";

    private MdcKafkaUtils() {
    }

    public static void setMdcFromRecord(ConsumerRecord<String, ?> record) {
        MDC.clear();

        Header traceIdHeader = record.headers().lastHeader(TRACE_ID_HEADER);
        if (traceIdHeader != null) {
            MDC.put(MdcLoggingFilter.TRACE_ID, new String(traceIdHeader.value(), StandardCharsets.UTF_8));
        }

        Header spanIdHeader = record.headers().lastHeader(SPAN_ID_HEADER);
        if (spanIdHeader != null) {
            MDC.put(MdcLoggingFilter.SPAN_ID, new String(spanIdHeader.value(), StandardCharsets.UTF_8));
        }
    }

    public static void clearMdc() {
        MDC.clear();
    }
}
