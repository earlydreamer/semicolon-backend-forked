package dukku.common.global.logging;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.MDC;
import org.springframework.kafka.listener.BatchInterceptor;

@Slf4j
public class KafkaTracingBatchInterceptor implements BatchInterceptor<String, String> {

    @Override
    public ConsumerRecords<String, String> intercept(ConsumerRecords<String, String> records,
                                                      Consumer<String, String> consumer) {
        MDC.clear();
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
