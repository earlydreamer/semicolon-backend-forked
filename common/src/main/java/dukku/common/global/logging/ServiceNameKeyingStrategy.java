package dukku.common.global.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.github.danielwegener.logback.kafka.keying.KeyingStrategy;

import java.nio.charset.StandardCharsets;

public class ServiceNameKeyingStrategy implements KeyingStrategy<ILoggingEvent> {

    private byte[] serviceNameBytes;

    public void setServiceName(String serviceName) {
        this.serviceNameBytes = serviceName.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] createKey(ILoggingEvent event) {
        return serviceNameBytes;
    }
}
