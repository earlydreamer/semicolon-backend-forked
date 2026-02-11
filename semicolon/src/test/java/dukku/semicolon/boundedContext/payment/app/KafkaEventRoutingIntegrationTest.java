package dukku.semicolon.boundedContext.payment.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.deposit.event.DepositDeductionFailedEvent;
import dukku.common.shared.order.event.PaymentRollbackRequestEvent;
import dukku.common.shared.payment.event.PaymentFailedEvent;
import dukku.common.shared.payment.event.PaymentSuccessEvent;
import dukku.common.shared.payment.event.RefundCompletedEvent;
import dukku.semicolon.boundedContext.deposit.app.DepositFacade;
import dukku.semicolon.boundedContext.order.app.UpdateOrderRefundStatusUseCase;
import dukku.semicolon.boundedContext.order.app.UpdateOrderStatusUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kafka 이벤트 라우팅 통합 테스트
 *
 * <p>
 * Payment/Deposit 도메인 이벤트의 Kafka 전환(Phase A: Spring Event + Kafka 동시 발행)이
 * 올바르게 동작하는지 검증한다. 검증 대상은 크게 4가지:
 *
 * <ol>
 * <li><b>Kafka 토픽 라우팅</b> — KafkaRoutableEvent를 구현한 이벤트만 올바른 토픽으로 발행되는지</li>
 * <li><b>Consumer 동작</b> — Kafka를 경유하여 실제 비즈니스 리스너에 도달하는지</li>
 * <li><b>Spring Event 하위호환</b> — Kafka 전환 후에도 Spring Event 경로가 유지되는지</li>
 * <li><b>트랜잭션 경계</b> — publishAfterCompletion이 롤백 시에도 Kafka 발행을 보장하는지</li>
 * </ol>
 *
 * <h3>비동기 테스트 주의사항</h3>
 * Kafka Consumer는 비동기로 메시지를 수신하므로, 이전 테스트에서 발행된 메시지가
 * 다음 테스트의 {@code @BeforeEach} 이후에 도착할 수 있다.
 * 이를 방지하기 위해 {@code setUp()}에서 대기 후 큐를 드레인한다.
 *
 * <h3>bootstrapServersProperty 설정 이유</h3>
 * {@code @EmbeddedKafka(bootstrapServersProperty = "spring.kafka.bootstrap-servers")}로
 * EmbeddedKafka가 직접 {@code spring.kafka.bootstrap-servers}에 broker 주소를 주입한다.
 * {@code @SpringBootTest(properties)} 방식의
 * {@code ${spring.embedded.kafka.brokers}}
 * 중첩 placeholder를 사용하면 logback-spring.xml의 {@code <springProperty>} 해석 시점에
 * 아직 프로퍼티가 등록되지 않아 Logback 초기화가 실패한다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:kafkatest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.hikari.connection-timeout=3000"
})
@EmbeddedKafka(partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@Import(KafkaEventRoutingIntegrationTest.TestConfig.class)
class KafkaEventRoutingIntegrationTest {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TestConfig testConfig;

    // ── Mock 설정 ──────────────────────────────────────────────────────
    // 비즈니스 Facade/UseCase를 Mock으로 대체하여,
    // Kafka Consumer(실제 리스너)가 호출하는 비즈니스 로직 실행을 차단한다.
    // 테스트 목적은 "이벤트가 리스너에 도달하는가"이므로 호출 여부만 검증한다.

    @MockitoBean
    private DepositFacade depositFacade;

    @MockitoBean
    private PaymentFacade paymentFacade;

    @MockitoBean
    private PaymentSupport paymentSupport;

    @MockitoBean
    private UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @MockitoBean
    private UpdateOrderRefundStatusUseCase updateOrderRefundStatusUseCase;

    // 애플리케이션 시작 시 실행되는 초기 데이터 로더 비활성화
    @MockitoBean(name = "initUsers")
    private CommandLineRunner initUsers;

    @MockitoBean(name = "initSystemDeposit")
    private CommandLineRunner initSystemDeposit;

    @MockitoBean(name = "initProducts")
    private CommandLineRunner initProducts;

    @MockitoBean(name = "initCategories")
    private CommandLineRunner initCategories;

    /**
     * 각 테스트 실행 전 캡처 큐를 드레인한다.
     *
     * <p>
     * Kafka Consumer는 비동기로 동작하므로, 이전 테스트에서 발행된 메시지가
     * 다음 테스트 시작 후에 도착할 수 있다. 예를 들어:
     * 
     * <pre>
     * Test A: PaymentFailedEvent 발행 → poll로 수신 확인 → 테스트 통과
     * Test B: @BeforeEach clear() 실행
     *         ... 이 시점에 Test A의 메시지가 TestConfig 큐에 도착 (비동기 지연)
     *         NonRoutable 이벤트 발행 → paymentFailedEvents.poll() → 잔여 메시지 수신 → 실패!
     * </pre>
     * 
     * 500ms 대기 후 clear()를 호출하여, 이전 테스트의 비동기 메시지가
     * 큐에 도착할 시간을 확보한 뒤 드레인한다.
     */
    @BeforeEach
    void setUp() throws InterruptedException {
        Thread.sleep(500);
        testConfig.clear();
    }

    // ── 1. Kafka 토픽 라우팅 검증 ──────────────────────────────────────
    // EventPublisher.publish() 호출 시 KafkaRoutableEvent를 구현한 이벤트가
    // 올바른 Kafka 토픽으로 발행되는지, TestConfig의 캡처 큐로 검증한다.

    @Test
    @DisplayName("PaymentSuccessEvent가 Kafka payment.success 토픽으로 라우팅된다")
    void routesPaymentSuccessEventToKafkaTopic() throws InterruptedException {
        // given
        var event = createPaymentSuccessEvent();

        // when
        publishInTransaction(event);

        // then — test-capture 그룹이 payment.success 토픽에서 수신
        assertThat(testConfig.paymentSuccessEvents.poll(5, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    @DisplayName("PaymentFailedEvent가 Kafka payment.failed 토픽으로 라우팅된다")
    void routesPaymentFailedEventToKafkaTopic() throws InterruptedException {
        // given & when
        publishInTransaction(new PaymentFailedEvent(UUID.randomUUID(), UUID.randomUUID(), "test"));

        // then — test-capture 그룹이 payment.failed 토픽에서 수신
        assertThat(testConfig.paymentFailedEvents.poll(5, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    @DisplayName("DepositDeductionFailedEvent가 Kafka deposit.deduction-failed 토픽으로 라우팅된다")
    void routesDepositDeductionFailedEventToKafkaTopic() throws InterruptedException {
        // given & when
        publishInTransaction(new DepositDeductionFailedEvent(
                UUID.randomUUID(), UUID.randomUUID(), 1000L, "test"));

        // then — test-capture 그룹이 deposit.deduction-failed 토픽에서 수신
        assertThat(testConfig.depositDeductionFailedEvents.poll(5, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    @DisplayName("RefundCompletedEvent가 Kafka payment.refund-completed 토픽으로 라우팅된다")
    void routesRefundCompletedEventToKafkaTopic() throws InterruptedException {
        // given & when
        publishInTransaction(new RefundCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                10000L, 5000L, UUID.randomUUID(), LocalDateTime.now()));

        // then — test-capture 그룹이 payment.refund-completed 토픽에서 수신
        assertThat(testConfig.refundCompletedEvents.poll(5, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    @DisplayName("KafkaRoutableEvent를 구현하지 않은 이벤트는 Kafka로 발행되지 않는다")
    void doesNotRouteNonRoutableEventToKafka() throws InterruptedException {
        // given — KafkaRoutableEvent를 구현하지 않은 이벤트
        record NonRoutableEvent(String data) {
        }

        // when
        publishInTransaction(new NonRoutableEvent("test"));

        // NonRoutable 이벤트는 Kafka로 발행되지 않으므로 큐에 아무것도 도착하지 않아야 한다.
        // 2초 대기: 만약 잘못 발행되었다면 이 시간 안에 수신될 것이다.
        // then — Kafka로 발행되지 않으므로 어떤 캡처 큐에도 도착하지 않아야 한다
        assertThat(testConfig.paymentSuccessEvents.poll(2, TimeUnit.SECONDS)).isNull();
        assertThat(testConfig.paymentFailedEvents.poll(200, TimeUnit.MILLISECONDS)).isNull();
    }

    // ── 2. @KafkaListener Consumer 동작 검증 ──────────────────────────
    // 이벤트가 Kafka를 경유하여 실제 비즈니스 리스너(@KafkaListener)에 도달하는지 검증.
    // Facade/UseCase는 @MockitoBean이므로 verify(mock, timeout)으로 호출 여부만 확인한다.
    // 각 테스트가 고유 UUID를 사용하므로 eq() 매칭으로 이전 테스트 메시지와 구분된다.

    @Test
    @DisplayName("PaymentSuccessEvent가 Kafka를 경유하여 DepositEventListener에 도달한다")
    void kafkaDeliversPaymentSuccessToDepositListener() {
        // given
        PaymentSuccessEvent event = createPaymentSuccessEvent();

        // when
        publishInTransaction(event);

        // then — DepositEventListener가 Kafka 경유로 이벤트를 수신하여 예치금 차감 호출
        verify(depositFacade, timeout(5000)).deductDepositForPayment(
                eq(event.userUuid()), eq(event.paymentDeposit()),
                eq(event.orderUuid()), eq(event.paymentUuid()), any());
    }

    @Test
    @DisplayName("DepositDeductionFailedEvent가 Kafka를 경유하여 PaymentEventListener에 도달한다")
    void kafkaDeliversDepositDeductionFailedToPaymentListener() {
        // given
        UUID orderUuid = UUID.randomUUID();

        // when
        publishInTransaction(new DepositDeductionFailedEvent(
                orderUuid, UUID.randomUUID(), 1000L, "잔액 부족"));

        // then — PaymentEventListener가 Kafka 경유로 이벤트를 수신하여 보상 트랜잭션 호출
        verify(paymentFacade, timeout(5000)).compensatePayment(eq(orderUuid), eq("잔액 부족"));
    }

    @Test
    @DisplayName("PaymentRollbackRequestEvent가 Kafka를 경유하여 PaymentEventListener에 도달한다")
    void kafkaDeliversPaymentRollbackRequestToPaymentListener() {
        // given
        UUID orderUuid = UUID.randomUUID();
        when(paymentSupport.findPaymentsByOrderUuid(orderUuid)).thenReturn(List.of());

        // when
        publishInTransaction(new PaymentRollbackRequestEvent(orderUuid, "주문 실패"));

        // then — PaymentEventListener가 Kafka 경유로 이벤트를 수신하여 결제 조회 호출
        verify(paymentSupport, timeout(5000)).findPaymentsByOrderUuid(eq(orderUuid));
    }

    // ── 3. Spring Event 하위호환 검증 ─────────────────────────────────
    // EventPublisher.publish()는 Spring Event도 동시에 발행한다.
    // @TransactionalEventListener 기반의 OrderEventListener가 여전히 동작하는지 검증한다.
    // Kafka 전환이 완료되지 않은 BC(Order 등)에서 Spring Event 경로가 유지되어야 한다.

    @Test
    @DisplayName("PaymentSuccessEvent가 Spring Event로도 발행되어 OrderEventListener에 도달한다")
    void springEventDeliversPaymentSuccessToOrderListener() {
        // given
        PaymentSuccessEvent event = createPaymentSuccessEvent();

        // when
        publishInTransaction(event);

        // then — OrderEventListener가 Spring Event로 수신하여 주문 상태 확정 호출
        verify(updateOrderStatusUseCase, timeout(5000)).confirmPayment(eq(event.orderUuid()));
    }

    @Test
    @DisplayName("PaymentFailedEvent가 Spring Event로도 발행되어 OrderEventListener에 도달한다")
    void springEventDeliversPaymentFailedToOrderListener() {
        // given
        PaymentFailedEvent event = new PaymentFailedEvent(UUID.randomUUID(), UUID.randomUUID(), "test");

        // when
        publishInTransaction(event);

        // then — OrderEventListener가 Spring Event로 수신하여 주문 실패 처리 호출
        verify(updateOrderStatusUseCase, timeout(5000)).failPayment(eq(event.orderUuid()));
    }

    // ── 4. publishAfterCompletion 검증 ────────────────────────────────
    // SAGA 보상 트랜잭션에서 사용하는 publishAfterCompletion()이
    // 트랜잭션 롤백 후에도 Kafka 발행을 보장하는지 검증한다.
    // 예: DeductDepositForPaymentUseCase에서 예치금 차감 실패 시
    // setRollbackOnly() → 롤백 → DepositDeductionFailedEvent가 발행되어야
    // PaymentEventListener가 보상 트랜잭션을 시작할 수 있다.

    @Test
    @DisplayName("publishAfterCompletion은 롤백 시에도 Kafka로 발행된다")
    void publishAfterCompletionSendsToKafkaEvenOnRollback() throws InterruptedException {
        // given
        DepositDeductionFailedEvent event = new DepositDeductionFailedEvent(
                UUID.randomUUID(), UUID.randomUUID(), 5000L, "롤백 테스트");

        // when — 트랜잭션을 강제 롤백
        new TransactionTemplate(transactionManager).execute(status -> {
            eventPublisher.publishAfterCompletion(event);
            status.setRollbackOnly();
            return null;
        });

        // then — 롤백 후에도 Kafka로 발행되었음을 확인
        assertThat(testConfig.depositDeductionFailedEvents.poll(5, TimeUnit.SECONDS)).isNotNull();
    }

    // ── Helper ────────────────────────────────────────────────────────

    /**
     * 트랜잭션 내에서 이벤트를 발행한다.
     * EventPublisher.publish()는 트랜잭션 커밋 후 Kafka 발행을 수행하므로,
     * 테스트에서도 트랜잭션 컨텍스트가 필요하다.
     */
    private void publishInTransaction(Object event) {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> eventPublisher.publish(event));
    }

    private PaymentSuccessEvent createPaymentSuccessEvent() {
        return new PaymentSuccessEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                20000L, 5000L, 15000L, UUID.randomUUID(), LocalDateTime.now(),
                List.of(new PaymentSuccessEvent.ItemDepositUsage(UUID.randomUUID(), 15000L)));
    }

    // ── 캡처용 TestConfig ─────────────────────────────────────────────

    /**
     * Kafka 토픽별 캡처 리스너.
     *
     * <p>
     * 실제 비즈니스 리스너(DepositEventListener, PaymentEventListener)와
     * <b>별도의 consumer group(test-capture)</b>으로 동작하여,
     * 비즈니스 리스너의 소비와 독립적으로 메시지를 수신한다.
     * 테스트에서 {@code poll(timeout)}으로 특정 토픽에 메시지가 도착했는지 확인한다.
     */
    @TestConfiguration
    @EnableAsync
    static class TestConfig {
        final BlockingQueue<PaymentSuccessEvent> paymentSuccessEvents = new LinkedBlockingQueue<>();
        final BlockingQueue<PaymentFailedEvent> paymentFailedEvents = new LinkedBlockingQueue<>();
        final BlockingQueue<DepositDeductionFailedEvent> depositDeductionFailedEvents = new LinkedBlockingQueue<>();
        final BlockingQueue<RefundCompletedEvent> refundCompletedEvents = new LinkedBlockingQueue<>();

        @KafkaListener(topics = PaymentSuccessEvent.TOPIC, groupId = "test-capture")
        void capturePaymentSuccess(PaymentSuccessEvent event) {
            paymentSuccessEvents.add(event);
        }

        @KafkaListener(topics = PaymentFailedEvent.TOPIC, groupId = "test-capture")
        void capturePaymentFailed(PaymentFailedEvent event) {
            paymentFailedEvents.add(event);
        }

        @KafkaListener(topics = DepositDeductionFailedEvent.TOPIC, groupId = "test-capture")
        void captureDepositDeductionFailed(DepositDeductionFailedEvent event) {
            depositDeductionFailedEvents.add(event);
        }

        @KafkaListener(topics = RefundCompletedEvent.TOPIC, groupId = "test-capture")
        void captureRefundCompleted(RefundCompletedEvent event) {
            refundCompletedEvents.add(event);
        }

        void clear() {
            paymentSuccessEvents.clear();
            paymentFailedEvents.clear();
            depositDeductionFailedEvents.clear();
            refundCompletedEvents.clear();
        }
    }
}
