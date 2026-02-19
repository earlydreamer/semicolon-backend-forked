package dukku.logconsumer.consumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class LogMetrics {

    private final MeterRegistry meterRegistry;

    public LogMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementLogCount(String serviceName, String level) {
        Counter.builder("log_messages_total")
                .tag("service", serviceName != null ? serviceName : "unknown")
                .tag("level", level != null ? level : "unknown")
                .description("서비스별/레벨별 수신 로그 메시지 수")
                .register(meterRegistry)
                .increment();
    }
}
