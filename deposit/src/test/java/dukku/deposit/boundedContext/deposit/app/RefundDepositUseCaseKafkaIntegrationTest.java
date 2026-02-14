package dukku.deposit.boundedContext.deposit.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dukku.common.shared.deposit.type.DepositHistoryType;
import dukku.deposit.boundedContext.deposit.entity.Deposit;
import dukku.deposit.boundedContext.deposit.entity.DepositHistory;
import dukku.deposit.boundedContext.deposit.out.DepositHistoryRepository;
import dukku.deposit.boundedContext.deposit.out.DepositRepository;
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
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.application.name=deposit-it",
        "spring.datasource.url=jdbc:h2:mem:deposit_kafka_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.listener.auto-startup=true",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.elasticsearch.uris=http://localhost:9200",
        "jwt.access.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktYWNjZXNzLTAxMjM=",
        "jwt.refresh.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktcmVmcmVzaC0wMTI=",
        "crypto.key=dGhpcy1rZXktaXMtdGVzdC1rZXktY3J5cHRvLTAxMjM="
})
@EmbeddedKafka(partitions = 1, topics = {
        "payment.refund-requested",
        "deposit.refunded"
}, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@ActiveProfiles("test")
class RefundDepositUseCaseKafkaIntegrationTest {

    private static final String DEPOSIT_REFUNDED_TOPIC = "deposit.refunded";

    @Autowired
    private RefundDepositUseCase refundDepositUseCase;

    @Autowired
    private DepositRepository depositRepository;

    @Autowired
    private DepositHistoryRepository depositHistoryRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        depositHistoryRepository.deleteAll();
        depositRepository.deleteAll();
    }

    @Test
    @DisplayName("?섎텋 濡ㅻ갚 ??Kafka??deposit.refunded ?대깽?멸? 諛쒗뻾?섍퀬 ?ъ슜???덉튂湲덉씠 利앷??쒕떎")
    void publishesDepositRefundEventAndIncreasesBalance() throws Exception {
        // given: ?좎? ?덉튂湲?5,000???곹깭瑜?留뚮뱺??
        UUID userUuid = UUID.randomUUID();
        depositRepository.save(Deposit.builder()
                .userUuid(userUuid)
                .depositUuid(UUID.randomUUID())
                .balance(5000L)
                .version(0)
                .build());
        UUID paymentUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        UUID refundUuid = UUID.randomUUID();
        Long refundDepositAmount = 3000L;

        // when: refundDepositUseCase瑜??ㅽ뻾?댁꽌 refund ?좎뒪耳?댁뒪瑜?泥섎━?쒕떎.
        refundDepositUseCase.execute(userUuid, refundDepositAmount, orderUuid, paymentUuid, refundUuid);

        Consumer<String, String> consumer = createConsumer();
        try {
            // then: deposit.refunded ?대깽?멸? Kafka濡?諛쒗뻾?섍퀬 payload媛 湲곕? 媛믨낵 ?쇱튂?쒕떎.
            ConsumerRecord<String, String> record = waitForRecord(consumer, DEPOSIT_REFUNDED_TOPIC);
            JsonNode payload = objectMapper.readTree(record.value());

            assertThat(record.key()).isEqualTo(paymentUuid.toString());
            assertThat(payload.get("refundId").asText()).isEqualTo(refundUuid.toString());
            assertThat(payload.get("paymentUuid").asText()).isEqualTo(paymentUuid.toString());
            assertThat(payload.get("orderUuid").asText()).isEqualTo(orderUuid.toString());
            assertThat(payload.get("userUuid").asText()).isEqualTo(userUuid.toString());
            assertThat(payload.get("amount").asLong()).isEqualTo(refundDepositAmount);

            // then: 湲곗〈 ?덉튂湲?5,000?먯뿉 3,000?먯씠 媛?곕릺??8,000?먯씠 ?쒕떎.
            var after = depositRepository.findByUserUuid(userUuid).orElseThrow();
            assertThat(after.getBalance()).isEqualTo(8000L);

            List<DepositHistory> histories = depositHistoryRepository.findByUserUuidOrderByCreatedAtDesc(userUuid);
            assertThat(histories).isNotEmpty();
            DepositHistory lastHistory = histories.get(0);
            assertThat(lastHistory.getType()).isEqualTo(DepositHistoryType.ROLLBACK);
            assertThat(lastHistory.getAmount()).isEqualTo(refundDepositAmount);
            assertThat(lastHistory.getOrderItemUuid()).isEqualTo(orderUuid);
        } finally {
            consumer.close();
        }
    }

    private Consumer<String, String> createConsumer() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "deposit-kafka-it-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new StringDeserializer()).createConsumer();

        List<TopicPartition> partitions = List.of(new TopicPartition(DEPOSIT_REFUNDED_TOPIC, 0));
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

        throw new IllegalStateException("No record found for topic: " + topic);
    }
}
