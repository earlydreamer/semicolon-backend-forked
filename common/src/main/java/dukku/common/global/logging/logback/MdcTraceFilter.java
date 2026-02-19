package dukku.common.global.logging.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.Map;

/**
 * MDC에 traceId가 존재하는 로그만 통과시키는 Logback 필터.
 * Kafka appender에 적용하여 HTTP 요청 컨텍스트가 있는 로그만 전송한다.
 */
public class MdcTraceFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc == null) {
            return FilterReply.DENY;
        }

        String traceId = mdc.get("traceId");
        if (traceId == null || traceId.isBlank()) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }
}
