package dukku.common.global.logging.kafaka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.RecordInterceptor;

@Slf4j
public class KafkaTracingRecordInterceptor implements RecordInterceptor<String, String> {

    @Override
    public ConsumerRecord<String, String> intercept(ConsumerRecord<String, String> record,
                                                     Consumer<String, String> consumer) {
        MdcKafkaUtils.setMdcFromRecord(record);
        log.debug("MDC set from Kafka header: topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset());
        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<String, String> record,
                            Consumer<String, String> consumer) {
        MdcKafkaUtils.clearMdc();
    }

    @Override
    public void failure(ConsumerRecord<String, String> record, Exception exception,
                        Consumer<String, String> consumer) {
        log.error("Kafka listener failed: topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset(), exception);
        MdcKafkaUtils.clearMdc();
    }
}
