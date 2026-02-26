package dukku.coupon.boundedContext.coupon.in;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dukku.common.shared.coupon.type.CouponStatus;
import dukku.common.shared.coupon.type.CouponUserStatus;
import dukku.common.shared.payment.event.PaymentFailedEvent;
import dukku.common.shared.payment.event.PaymentSuccessEvent;
import dukku.common.shared.payment.type.PaymentFailureCode;
import dukku.common.shared.payment.type.PaymentFailureStage;
import dukku.coupon.boundedContext.coupon.entity.Coupon;
import dukku.coupon.boundedContext.coupon.entity.CouponUser;
import dukku.coupon.boundedContext.coupon.out.CouponRepository;
import dukku.coupon.boundedContext.coupon.out.CouponUserRepository;
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
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:coupon_kafka_saga_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.embedded.kafka.brokers.property=spring.kafka.bootstrap-servers",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.listener.auto-startup=true",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.elasticsearch.uris=http://localhost:9200",
        "jwt.access.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktYWNjZXNzLTAxMjM=",
        "jwt.refresh.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktcmVmcmVzaC0wMTI=",
        "crypto.key=dGhpcy1rZXktaXMtdGVzdC1rZXktY3J5cHRvLTAxMjM="
})
@EmbeddedKafka(partitions = 1, topics = {
        "payment.success",
        "payment.failed",
        "payment.rollback"
}, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class CouponSagaKafkaIntegrationTest {

    private static final String PAYMENT_SUCCESS_TOPIC = "payment.success";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";
    private static final String PAYMENT_ROLLBACK_TOPIC = "payment.rollback";

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponUserRepository couponUserRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @BeforeEach
    void cleanUp() {
        kafkaListenerEndpointRegistry.getListenerContainers()
                .forEach(container -> ContainerTestUtils.waitForAssignment(
                        container,
                        embeddedKafkaBroker.getPartitionsPerTopic()));
        couponUserRepository.deleteAll();
        couponRepository.deleteAll();
    }

    @Test
    @DisplayName("Kafka payment.success -> payment.failed 보상 시 쿠폰 상태가 USED -> AVAILABLE로 복구된다")
    void restoresCouponStatusThroughKafkaSagaFlow() throws Exception {
        UUID userUuid = UUID.randomUUID();
        UUID couponUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();

        Coupon coupon = couponRepository.save(activeCoupon(couponUuid));
        couponUserRepository.save(CouponUser.create(userUuid, coupon));

        PaymentSuccessEvent successEvent = new PaymentSuccessEvent(
                paymentUuid,
                orderUuid,
                12000L,
                9000L,
                3000L,
                userUuid,
                LocalDateTime.now(),
                List.of(),
                couponUuid);

        sendEvent(PAYMENT_SUCCESS_TOPIC, orderUuid.toString(), successEvent);
        awaitCouponStatus(userUuid, couponUuid, CouponUserStatus.USED, Duration.ofSeconds(8));

        PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                orderUuid,
                paymentUuid,
                userUuid,
                PaymentFailureStage.DEPOSIT_DEDUCTION,
                PaymentFailureCode.DEPOSIT_DEDUCTION_FAILED,
                false,
                "compensation",
                LocalDateTime.now(),
                couponUuid);

        sendEvent(PAYMENT_FAILED_TOPIC, orderUuid.toString(), failedEvent);
        awaitCouponStatus(userUuid, couponUuid, CouponUserStatus.AVAILABLE, Duration.ofSeconds(8));
    }

    @Test
    @DisplayName("Kafka payment.success 처리 중 쿠폰 적용 실패 시 payment.rollback 이벤트를 발행한다")
    void publishesRollbackEventThroughKafkaOnApplyFailure() throws Exception {
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        UUID missingCouponUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();

        PaymentSuccessEvent successEvent = new PaymentSuccessEvent(
                paymentUuid,
                orderUuid,
                12000L,
                9000L,
                3000L,
                userUuid,
                LocalDateTime.now(),
                List.of(),
                missingCouponUuid);

        try (Consumer<String, String> consumer = createConsumerForRollbackTopic()) {
            sendEvent(PAYMENT_SUCCESS_TOPIC, orderUuid.toString(), successEvent);
            ConsumerRecord<String, String> rollbackRecord = waitForRecord(
                    consumer,
                    PAYMENT_ROLLBACK_TOPIC,
                    Duration.ofSeconds(20));

            JsonNode rollbackJson = objectMapper.readTree(rollbackRecord.value());
            assertThat(rollbackRecord.key()).isEqualTo(orderUuid.toString());
            assertThat(rollbackJson.get("orderUuid").asText()).isEqualTo(orderUuid.toString());
            assertThat(rollbackJson.get("reason").asText()).isNotBlank();
        }
    }

    private void sendEvent(String topic, String key, Object event) throws Exception {
        ObjectNode payloadNode = objectMapper.valueToTree(event);
        payloadNode.remove("topic");
        payloadNode.remove("key");
        String payload = objectMapper.writeValueAsString(payloadNode);
        kafkaTemplate.send(topic, key, payload).get(5, TimeUnit.SECONDS);
    }

    private void awaitCouponStatus(
            UUID userUuid,
            UUID couponUuid,
            CouponUserStatus expectedStatus,
            Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() <= deadline) {
            CouponUser couponUser = couponUserRepository.findByUserUuidAndCoupon_Uuid(userUuid, couponUuid)
                    .orElseThrow();
            if (couponUser.getStatus() == expectedStatus) {
                return;
            }
            sleep(200);
        }

        CouponUser current = couponUserRepository.findByUserUuidAndCoupon_Uuid(userUuid, couponUuid).orElseThrow();
        fail("쿠폰 상태 대기 타임아웃. expected=" + expectedStatus + ", actual=" + current.getStatus());
    }

    private Consumer<String, String> createConsumerForRollbackTopic() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "coupon-kafka-it-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new StringDeserializer()).createConsumer();

        TopicPartition rollbackPartition = new TopicPartition(PAYMENT_ROLLBACK_TOPIC, 0);
        consumer.assign(List.of(rollbackPartition));
        consumer.seekToEnd(List.of(rollbackPartition));

        return consumer;
    }

    private ConsumerRecord<String, String> waitForRecord(
            Consumer<String, String> consumer,
            String topic,
            Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() <= deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(300));
            for (ConsumerRecord<String, String> record : records) {
                if (topic.equals(record.topic())) {
                    return record;
                }
            }
        }

        return fail("토픽에서 이벤트 수신 실패: " + topic);
    }

    private Coupon activeCoupon(UUID couponUuid) {
        return Coupon.builder()
                .uuid(couponUuid)
                .couponName("saga-coupon")
                .discountAmount(1000)
                .minimumOrderAmount(1000)
                .validFrom(LocalDateTime.now().minusDays(1))
                .status(CouponStatus.ACTIVE)
                .totalQuantity(100)
                .issuedQuantity(1)
                .build();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

}
