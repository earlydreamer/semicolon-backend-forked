package dukku.payment.boundedContext.payment.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.order.event.OrderItemCanceledEvent;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.common.shared.payment.type.PaymentType;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.payment.boundedContext.payment.entity.PaymentOrderItem;
import dukku.payment.boundedContext.payment.out.PaymentHistoryRepository;
import dukku.payment.boundedContext.payment.out.PaymentRepository;
import dukku.payment.boundedContext.payment.out.RefundRepository;
import dukku.payment.boundedContext.payment.out.TossPaymentClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:payment_cancel_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "toss.api.secret-key=test-key",
        "JWT_ACCESS_SECRET=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=",
        "JWT_REFRESH_SECRET=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=",
        "CRYPTO_KEY=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.embedded.kafka.brokers.property=spring.kafka.bootstrap-servers",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.listener.auto-startup=true"
})
@EmbeddedKafka(partitions = 1, topics = {
        "order.item.canceled",
        "payment.refund-completed"
}, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@ActiveProfiles("test")
public class OrderCancellationIntegrationTest {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private TossPaymentClient tossPaymentClient;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        kafkaListenerEndpointRegistry.getListenerContainers()
                .forEach(container -> ContainerTestUtils.waitForAssignment(
                        container,
                        embeddedKafkaBroker.getPartitionsPerTopic()));

        transactionTemplate = new TransactionTemplate(transactionManager);
        paymentHistoryRepository.deleteAll();
        refundRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("주문 상품 취소 이벤트 수신 시 결제 환불 처리 및 결제 상태 CANCELED 전환 확인")
    void handlesOrderItemCanceledEventAndRefunds() {
        // given: 1. 결제 데이터 준비 (상품 1개 포함)
        UUID orderUuid = UUID.randomUUID();
        UUID orderItemUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        long amount = 10000L;

        Payment payment = transactionTemplate.execute(status -> {
            Payment p = Payment.create(orderUuid, userUuid, amount, 0L, amount, 0L, PaymentType.NORMAL,
                    "toss-order-id-" + orderUuid);
            p.approve("pg-key-123");

            PaymentOrderItem item = PaymentOrderItem.create(p, orderUuid, orderItemUuid, 1, "테스트 상품", amount, 0L,
                    UUID.randomUUID(), 0L);
            p.addItem(item);

            return paymentRepository.save(p);
        });

        // given: 2. PG 취소 응답 Mocking
        when(tossPaymentClient.cancel(eq("pg-key-123"), anyMap())).thenReturn(Map.of("statusCode", 200));

        // when: 3. 주문 취소 이벤트 발행
        OrderItemCanceledEvent event = new OrderItemCanceledEvent(orderItemUuid);
        eventPublisher.publish(event);

        // then: 4. 결제 상태 CANCELED 전환 확인 (비동기 처리 대기)
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Payment updatedPayment = paymentRepository.findByUuid(payment.getUuid()).orElseThrow();
            assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
            assertThat(updatedPayment.getRefundTotal()).isEqualTo(amount);
        });

        // then: 5. 환불 엔티티 생성 확인
        assertThat(refundRepository.count()).isEqualTo(1);
    }
}
