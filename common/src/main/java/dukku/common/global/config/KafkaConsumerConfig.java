package dukku.common.global.config;

import dukku.common.global.logging.KafkaTracingRecordInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

/**
 * Kafka Consumer 설정
 *
 * <p>Spring Kafka의 {@code ConcurrentKafkaListenerContainerFactory}에
 * {@code KafkaTracingRecordInterceptor}를 등록하여
 * 레코드 단위 MDC 라이프사이클을 보장합니다.</p>
 *
 * <p>테스트 환경 등 Kafka 자동 설정이 비활성화된 경우
 * {@code ConsumerFactory} 빈이 없으므로 팩토리 생성을 건너뜁니다.</p>
 */
@Configuration
@ConditionalOnClass(ConcurrentKafkaListenerContainerFactory.class)
public class KafkaConsumerConfig {

    @Bean
    @ConditionalOnBean(ConsumerFactory.class)
    public KafkaTracingRecordInterceptor kafkaTracingRecordInterceptor() {
        return new KafkaTracingRecordInterceptor();
    }

    @Bean
    @ConditionalOnBean(ConsumerFactory.class)
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTracingRecordInterceptor kafkaTracingRecordInterceptor) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setRecordInterceptor(kafkaTracingRecordInterceptor);
        return factory;
    }
}
