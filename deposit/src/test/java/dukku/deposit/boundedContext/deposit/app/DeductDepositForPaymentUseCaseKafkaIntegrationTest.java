package dukku.deposit.boundedContext.deposit.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dukku.deposit.boundedContext.deposit.entity.Deposit;
import dukku.deposit.boundedContext.deposit.out.DepositHistoryRepository;
import dukku.deposit.boundedContext.deposit.out.DepositRepository;
import dukku.deposit.global.SystemDepositInitData;
import dukku.common.shared.payment.event.PaymentSuccessEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.application.name=deposit-it",
        "spring.datasource.url=jdbc:h2:mem:deduct_kafka_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.listener.auto-startup=false",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.elasticsearch.uris=http://localhost:9200",
        "jwt.access.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktYWNjZXNzLTAxMjM=",
        "jwt.refresh.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktcmVmcmVzaC0wMTI=",
        "crypto.key=dGhpcy1rZXktaXMtdGVzdC1rZXktY3J5cHRvLTAxMjM="
})
@EmbeddedKafka(partitions = 1, topics = {
        "deposit.deduction-failed"
}, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class DeductDepositForPaymentUseCaseKafkaIntegrationTest {

    private static final String DEDUCT_FAIL_TOPIC = "deposit.deduction-failed";

    @Autowired
    private DeductDepositForPaymentUseCase useCase;

    @Autowired
    private DepositRepository depositRepository;

    @Autowired
    private DepositHistoryRepository historyRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        historyRepository.deleteAll();
        depositRepository.deleteAll();
    }

    @Test
    @DisplayName("사용자 잔액 부족이면 deduction failed 이벤트가 Kafka로 발행된다")
    void failSendsEvent() throws Exception {
        UUID userUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID orderItemUuid = UUID.randomUUID();

        saveDeposit(userUuid, 1000L);
        saveDeposit(SystemDepositInitData.SYSTEM_USER_UUID, 1_000_000L);

        List<PaymentSuccessEvent.ItemDepositUsage> usages = List.of(
                new PaymentSuccessEvent.ItemDepositUsage(orderItemUuid, 3000L));

        assertThatCode(() -> useCase.execute(userUuid, 3000L, orderUuid, paymentUuid, usages))
                .doesNotThrowAnyException();

        Consumer<String, String> consumer = createConsumer(DEDUCT_FAIL_TOPIC);
        try {
            ConsumerRecord<String, String> record = waitForRecord(consumer, DEDUCT_FAIL_TOPIC);
            JsonNode payload = objectMapper.readTree(record.value());

            assertThat(record.key()).isEqualTo(orderUuid.toString());
            assertThat(payload.get("orderUuid").asText()).isEqualTo(orderUuid.toString());
            assertThat(payload.get("paymentUuid").asText()).isEqualTo(paymentUuid.toString());
            assertThat(payload.get("userUuid").asText()).isEqualTo(userUuid.toString());
            assertThat(payload.get("amount").asLong()).isEqualTo(3000L);
            assertThat(payload.get("failureCode").asText()).isEqualTo("BALANCE_SHORTAGE");
            assertThat(payload.get("retryable").asBoolean()).isFalse();

            assertThat(depositRepository.findByUserUuid(userUuid).orElseThrow().getBalance()).isEqualTo(1000L);
            assertThat(
                    depositRepository.findByUserUuid(SystemDepositInitData.SYSTEM_USER_UUID).orElseThrow().getBalance())
                    .isEqualTo(1_000_000L);
            assertThat(historyRepository.findByUserUuidOrderByCreatedAtDesc(userUuid)).isEmpty();
            assertThat(historyRepository.findByUserUuidOrderByCreatedAtDesc(SystemDepositInitData.SYSTEM_USER_UUID))
                    .isEmpty();
        } finally {
            consumer.close();
        }
    }

    private void saveDeposit(UUID userUuid, Long balance) {
        depositRepository.save(Deposit.builder()
                .userUuid(userUuid)
                .depositUuid(UUID.randomUUID())
                .balance(balance)
                .version(0)
                .build());
    }

    private Consumer<String, String> createConsumer(String topic) {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "deposit-kafka-it-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new StringDeserializer()).createConsumer();

        List<TopicPartition> partitions = List.of(new TopicPartition(topic, 0));
        consumer.assign(partitions);
        consumer.seekToBeginning(partitions);

        return consumer;
    }

    private ConsumerRecord<String, String> waitForRecord(Consumer<String, String> consumer, String topic) {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
        while (System.currentTimeMillis() <= deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(300));
            for (ConsumerRecord<String, String> record : records) {
                if (record.topic().equals(topic)) {
                    return record;
                }
            }
        }

        return fail("No record found for topic: " + topic);
    }
}
