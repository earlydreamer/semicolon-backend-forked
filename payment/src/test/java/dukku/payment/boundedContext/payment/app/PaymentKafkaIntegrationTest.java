package dukku.payment.boundedContext.payment.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import dukku.common.shared.payment.dto.PaymentConfirmRequest;
import dukku.common.shared.payment.dto.PaymentRefundRequest;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.common.shared.payment.type.PaymentType;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.payment.boundedContext.payment.out.PaymentHistoryRepository;
import dukku.payment.boundedContext.payment.out.PaymentRepository;
import dukku.payment.boundedContext.payment.out.RefundRepository;
import dukku.payment.boundedContext.payment.out.TossPaymentClient;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:payment_kafka_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.embedded.kafka.brokers.property=spring.kafka.bootstrap-servers",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.listener.auto-startup=false",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.elasticsearch.uris=http://localhost:9200",
        "jwt.access.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktYWNjZXNzLTAxMjM=",
        "jwt.refresh.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktcmVmcmVzaC0wMTI=",
        "crypto.key=dGhpcy1rZXktaXMtdGVzdC1rZXktY3J5cHRvLTAxMjM=",
        "toss.api.secret-key=test-gsk-docs-key"
})
@EmbeddedKafka(partitions = 1, topics = {
        "payment.success",
        "payment.refund-completed"
}, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@ActiveProfiles("test")
class PaymentKafkaIntegrationTest {

    private static final String PAYMENT_SUCCESS_TOPIC = "payment.success";
    private static final String REFUND_COMPLETED_TOPIC = "payment.refund-completed";

    @Autowired
    private ConfirmPaymentUseCase confirmPaymentUseCase;

    @Autowired
    private RefundPaymentUseCase refundPaymentUseCase;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment environment;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private TossPaymentClient tossPaymentClient;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void cleanUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        paymentHistoryRepository.deleteAll();
        refundRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("결제 승인부터 전체 환불까지 수행하면 success/refund-completed 이벤트가 Kafka에 발행된다")
    void publishesKafkaEventsAcrossConfirmAndRefundFlow() throws Exception {
        // given: 테스트 컨텍스트가 Embedded Kafka 브로커를 사용하고 있는지 검증한다.
        assertThat(environment.getProperty("spring.kafka.bootstrap-servers"))
                .isEqualTo(embeddedKafkaBroker.getBrokersAsString());

        // given: 환불 가능한 PENDING 결제를 준비하고 PG confirm/cancel 응답을 성공으로 설정한다.
        Payment pendingPayment = createPendingPayment(12000L, "kafka-chain-order-1");
        when(tossPaymentClient.confirm(anyMap())).thenReturn(Map.of("statusCode", 200));
        when(tossPaymentClient.cancel(eq("pg-key-kafka-1"), anyMap())).thenReturn(Map.of("statusCode", 200));

        PaymentConfirmRequest confirmRequest = PaymentConfirmRequest.builder()
                .paymentUuid(pendingPayment.getUuid())
                .toss(PaymentConfirmRequest.TossConfirmInfo.builder()
                        .paymentKey("pg-key-kafka-1")
                        .orderId(pendingPayment.getTossOrderId())
                        .amount(12000L)
                        .build())
                .build();

        PaymentRefundRequest refundRequest = PaymentRefundRequest.builder()
                .paymentId(pendingPayment.getUuid())
                .orderUuid(pendingPayment.getOrderUuid())
                .refundAmount(12000L)
                .reason("customer_cancel_request")
                .build();

        // when: 결제를 승인하고 같은 결제에 대해 전체 환불을 수행한다.
        transactionTemplate.execute(status ->
                confirmPaymentUseCase.execute(confirmRequest, "idem-confirm-kafka-1"));
        refundPaymentUseCase.execute(refundRequest, "idem-refund-kafka-1");

        Consumer<String, String> consumer = createConsumer();
        try {
            // then: payment.success / payment.refund-completed 토픽 이벤트가 발행되고 payload 값이 일치한다.
            Map<String, ConsumerRecord<String, String>> recordsByTopic = waitForRecords(
                    consumer,
                    Set.of(PAYMENT_SUCCESS_TOPIC, REFUND_COMPLETED_TOPIC));

            ConsumerRecord<String, String> successRecord = recordsByTopic.get(PAYMENT_SUCCESS_TOPIC);
            JsonNode successJson = objectMapper.readTree(successRecord.value());

            assertThat(successRecord.key()).isEqualTo(pendingPayment.getOrderUuid().toString());
            assertThat(successJson.get("orderUuid").asText()).isEqualTo(pendingPayment.getOrderUuid().toString());
            assertThat(successJson.get("paymentUuid").asText()).isEqualTo(pendingPayment.getUuid().toString());
            assertThat(successJson.get("paymentId").asText()).isEqualTo(pendingPayment.getUuid().toString());
            assertThat(successJson.get("amount").asLong()).isEqualTo(12000L);
            assertThat(successJson.get("pgAmount").asLong()).isEqualTo(12000L);
            assertThat(successJson.get("paymentDeposit").asLong()).isEqualTo(0L);

            ConsumerRecord<String, String> refundRecord = recordsByTopic.get(REFUND_COMPLETED_TOPIC);
            JsonNode refundJson = objectMapper.readTree(refundRecord.value());

            assertThat(refundRecord.key()).isEqualTo(pendingPayment.getOrderUuid().toString());
            assertThat(refundJson.get("orderUuid").asText()).isEqualTo(pendingPayment.getOrderUuid().toString());
            assertThat(refundJson.get("paymentId").asText()).isEqualTo(pendingPayment.getUuid().toString());
            assertThat(refundJson.get("refundAmount").asLong()).isEqualTo(12000L);
            assertThat(refundJson.get("refundDepositAmount").asLong()).isEqualTo(0L);

            Payment canceledPayment = paymentRepository.findByUuid(pendingPayment.getUuid()).orElseThrow();
            assertThat(canceledPayment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
            assertThat(canceledPayment.getRefundTotal()).isEqualTo(12000L);
            assertThat(canceledPayment.getAmountPg()).isEqualTo(0L);
        } finally {
            consumer.close();
        }
    }

    private Consumer<String, String> createConsumer() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-kafka-it-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new StringDeserializer()).createConsumer();

        List<TopicPartition> partitions = List.of(
                new TopicPartition(PAYMENT_SUCCESS_TOPIC, 0),
                new TopicPartition(REFUND_COMPLETED_TOPIC, 0));
        consumer.assign(partitions);
        consumer.seekToBeginning(partitions);

        return consumer;
    }

    private Map<String, ConsumerRecord<String, String>> waitForRecords(
            Consumer<String, String> consumer,
            Set<String> requiredTopics) {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
        Map<String, ConsumerRecord<String, String>> foundRecords = new HashMap<>();

        while (System.currentTimeMillis() <= deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                if (requiredTopics.contains(record.topic()) && !foundRecords.containsKey(record.topic())) {
                    foundRecords.put(record.topic(), record);
                }
            }

            if (foundRecords.keySet().containsAll(requiredTopics)) {
                return foundRecords;
            }
        }

        throw new IllegalStateException("No records found for topics: " + requiredTopics);
    }

    private Payment createPendingPayment(Long amount, String tossOrderId) {
        return paymentRepository.save(Payment.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                amount,
                0L,
                amount,
                0L,
                PaymentType.NORMAL,
                tossOrderId));
    }
}

