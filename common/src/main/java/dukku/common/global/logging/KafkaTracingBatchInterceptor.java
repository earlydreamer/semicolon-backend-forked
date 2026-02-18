package dukku.common.global.logging;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.listener.BatchInterceptor;

import java.util.Iterator;

@Slf4j
public class KafkaTracingBatchInterceptor implements BatchInterceptor<String, String> {

    @Override
    public ConsumerRecords<String, String> intercept(ConsumerRecords<String, String> records,
                                                      Consumer<String, String> consumer) {
        // 배치의 첫 번째 레코드에서 traceId/spanId 추출
        Iterator<ConsumerRecord<String, String>> iterator = records.iterator();
        if (iterator.hasNext()) {
            MdcKafkaUtils.setMdcFromRecord(iterator.next());
        }

        log.debug("Kafka batch received: {} records", records.count());
        return records;
    }

    @Override
    public void success(ConsumerRecords<String, String> records, Consumer<String, String> consumer) {
        MdcKafkaUtils.clearMdc();
    }

    @Override
    public void failure(ConsumerRecords<String, String> records, Exception exception,
                        Consumer<String, String> consumer) {
        log.error("Kafka batch listener failed: {} records", records.count(), exception);
        MdcKafkaUtils.clearMdc();
    }
}
