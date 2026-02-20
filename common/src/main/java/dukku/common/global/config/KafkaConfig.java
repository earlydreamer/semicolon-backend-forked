package dukku.common.global.config;

import dukku.common.global.logging.kafka.KafkaTracingBatchInterceptor;
import dukku.common.global.logging.kafka.KafkaTracingRecordInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 전역 설정 클래스
 * 모든 모듈에서 사용하는 공통 Kafka Producer/Consumer 설정 관리
 * 특히 JSON 메시지의 자동 직렬화 및 역직렬화를 위한 표준 인프라 제공
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public KafkaTracingRecordInterceptor kafkaTracingRecordInterceptor() {
        return new KafkaTracingRecordInterceptor();
    }

    /**
     * JSON 메시지 변환기 빈 등록
     * 객체(DTO)와 Kafka 메시지(JSON String) 사이의 변환 담당
     * 빈 등록 시 @KafkaListener 파라미터로 DTO 타입 직접 사용 가능
     */
    @Bean
    public JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new JsonMessageConverter(objectMapper);
    }

    /**
     * 기본 Kafka 리스너 컨테이너 팩토리
     * RecordMessageConverter 전역 설정을 통한 개별 리스너의 자동 역직렬화 지원
     * JSON 데이터를 POJO/DTO로 즉시 수신 가능
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            KafkaTracingRecordInterceptor kafkaTracingRecordInterceptor,
            RecordMessageConverter recordMessageConverter) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setRecordInterceptor(kafkaTracingRecordInterceptor);
        factory.setRecordMessageConverter(recordMessageConverter);
        return factory;
    }

    @Bean
    public KafkaTracingBatchInterceptor kafkaTracingBatchInterceptor() {
        return new KafkaTracingBatchInterceptor();
    }

    @Bean("batchKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> batchKafkaListenerContainerFactory(
            KafkaTracingBatchInterceptor kafkaTracingBatchInterceptor) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(true);
        factory.setBatchInterceptor(kafkaTracingBatchInterceptor);
        return factory;
    }
}
