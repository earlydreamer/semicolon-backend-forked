package dukku.payment.boundedContext.payment.app;

import dukku.common.global.event.DomainEvent;
import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.deposit.event.DepositRefundFailedEvent;
import dukku.common.shared.deposit.event.DepositRefundedEvent;
import dukku.common.shared.deposit.type.DepositFailureCode;
import dukku.common.shared.payment.dto.PaymentRefundRequest;
import dukku.common.shared.payment.event.RefundCompletedEvent;
import dukku.common.shared.payment.event.RefundRequestedEvent;
import dukku.common.shared.payment.type.PaymentHistoryType;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.common.shared.payment.type.PaymentType;
import dukku.common.shared.payment.type.RefundStatus;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.payment.boundedContext.payment.entity.PaymentHistory;
import dukku.payment.boundedContext.payment.entity.Refund;
import dukku.payment.boundedContext.payment.in.PaymentEventListener;
import dukku.payment.boundedContext.payment.out.PaymentHistoryRepository;
import dukku.payment.boundedContext.payment.out.PaymentRepository;
import dukku.payment.boundedContext.payment.out.RefundItemRepository;
import dukku.payment.boundedContext.payment.out.RefundRepository;
import dukku.payment.boundedContext.payment.out.TossPaymentClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * deposit.refunded / deposit.refund.failed 이벤트를 기준으로 환불 Saga 흐름 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_refund_saga_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.elasticsearch.uris=http://localhost:9200",
        "jwt.access.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktYWNjZXNzLTAxMjM=",
        "jwt.refresh.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktcmVmcmVzaC0wMTI=",
        "crypto.key=dGhpcy1rZXktaXMtdGVzdC1rZXktY3J5cHRvLTAxMjM="
})
@Transactional
class RefundSagaLifecycleIntegrationTest {

    @Autowired
    private RefundPaymentUseCase refundPaymentUseCase;

    @Autowired
    private PaymentEventListener paymentEventListener;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private RefundItemRepository refundItemRepository;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    @MockitoBean
    private TossPaymentClient tossPaymentClient;

    @MockitoBean
    private EventPublisher eventPublisher;

    @BeforeEach
    void cleanUp() {
        paymentHistoryRepository.deleteAll();
        refundItemRepository.deleteAll();
        refundRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("deposit.refunded 이벤트에서 refund가 COMPLETED로 전환되고 Saga 흐름이 완료되는지 검증")
    void sagaCompletesWhenDepositRefunded() {
        // given: DONE 상태 결제에서 전체 환불 요청 준비
        Payment payment = createDonePayment(
                20000L,
                15000L,
                5000L,
                PaymentType.MIXED,
                "pg-key-saga-complete-1",
                "toss-order-saga-complete-1");

        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .paymentId(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .refundAmount(20000L)
                .reason("customer_cancel_request")
                .build();

        when(tossPaymentClient.cancel(eq("pg-key-saga-complete-1"), anyMap()))
                .thenReturn(Map.of("statusCode", 200));

        // when: 환불 요청 실행 후 deposit.refunded 이벤트 전달
        refundPaymentUseCase.execute(request, "idem-saga-complete-1");

        Refund refund = refundRepository.findByIdempotencyKey("idem-saga-complete-1").orElseThrow();
        assertThat(refund.getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        paymentEventListener.handle(new DepositRefundedEvent(
                refund.getUuid(),
                payment.getUuid(),
                payment.getOrderUuid(),
                payment.getUserUuid(),
                refund.getRefundDepositTotal()));

        // then: refund가 COMPLETED가 되고 이벤트가 순서대로 발행되는지 확인
        Refund updatedRefund = refundRepository.findByUuid(refund.getUuid()).orElseThrow();
        assertThat(updatedRefund.getRefundStatus()).isEqualTo(RefundStatus.COMPLETED);

        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publish(eventCaptor.capture());

        List<DomainEvent> events = eventCaptor.getAllValues();
        assertThat(events).hasSize(2);
        assertThat(events.get(0)).isInstanceOf(RefundRequestedEvent.class);
        assertThat(events.get(1)).isInstanceOf(RefundCompletedEvent.class);
        assertThat(events).extracting(DomainEvent::getTopic)
                .containsExactly("payment.refund-requested", "payment.refund-completed");
    }

    @Test
    @DisplayName("deposit.refunded 이벤트에서 부분 환불은 PARTIAL_CANCELED를 유지한 채 COMPLETED 되는지 검증")
    void partialSagaCompletesWhenDepositRefunded() {
        // given: 부분 환불 요청 준비
        Payment payment = createDonePayment(
                20000L,
                15000L,
                5000L,
                PaymentType.MIXED,
                "pg-key-saga-partial-complete-1",
                "toss-order-saga-partial-complete-1");

        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .paymentId(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .refundAmount(7000L)
                .reason("customer_partial_cancel")
                .build();

        // when: 환불 요청 실행 후 deposit.refunded 이벤트 전달
        refundPaymentUseCase.execute(request, "idem-saga-partial-complete-1");

        Refund refund = refundRepository.findByIdempotencyKey("idem-saga-partial-complete-1").orElseThrow();
        Payment afterRequest = paymentRepository.findByUuid(payment.getUuid()).orElseThrow();
        assertThat(refund.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(afterRequest.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIAL_CANCELED);

        paymentEventListener.handle(new DepositRefundedEvent(
                refund.getUuid(),
                payment.getUuid(),
                payment.getOrderUuid(),
                payment.getUserUuid(),
                refund.getRefundDepositTotal()));

        // then: refund만 COMPLETED로 바뀌고 payment는 PARTIAL_CANCELED 유지
        Refund updatedRefund = refundRepository.findByUuid(refund.getUuid()).orElseThrow();
        Payment updatedPayment = paymentRepository.findByUuid(payment.getUuid()).orElseThrow();
        assertThat(updatedRefund.getRefundStatus()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIAL_CANCELED);

        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publish(eventCaptor.capture());

        List<DomainEvent> events = eventCaptor.getAllValues();
        assertThat(events).hasSize(2);
        assertThat(events.get(0)).isInstanceOf(RefundRequestedEvent.class);
        assertThat(events.get(1)).isInstanceOf(RefundCompletedEvent.class);
        assertThat(events).extracting(DomainEvent::getTopic)
                .containsExactly("payment.refund-requested", "payment.refund-completed");
    }

    @Test
    @DisplayName("deposit.refund.failed 이벤트에서 FULL 환불이 ROLLBACK_FAILED로 전환되며 FULL_REFUND_FAILED 이력이 남는지 검증")
    void sagaFailureRecordsFullRefundFailedHistory() {
        // given: 전체 환불 요청 준비
        Payment payment = createDonePayment(
                20000L,
                15000L,
                5000L,
                PaymentType.MIXED,
                "pg-key-saga-fail-1",
                "toss-order-saga-fail-1");

        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .paymentId(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .refundAmount(20000L)
                .reason("customer_cancel_request")
                .build();

        when(tossPaymentClient.cancel(eq("pg-key-saga-fail-1"), anyMap()))
                .thenReturn(Map.of("statusCode", 200));

        // when: 환불 요청 실행 후 deposit.refund.failed 이벤트 전달
        refundPaymentUseCase.execute(request, "idem-saga-fail-1");

        Refund refund = refundRepository.findByIdempotencyKey("idem-saga-fail-1").orElseThrow();
        paymentEventListener.handle(new DepositRefundFailedEvent(
                refund.getUuid(),
                payment.getOrderUuid(),
                payment.getUuid(),
                payment.getUserUuid(),
                refund.getRefundDepositTotal(),
                DepositFailureCode.PERSISTENCE_ERROR,
                true,
                "forced failure",
                LocalDateTime.now()));

        // then: payment는 rollback-failed, refund는 canceled, 이력은 FULL_REFUND_FAILED 포함
        Payment updatedPayment = paymentRepository.findByUuid(payment.getUuid()).orElseThrow();
        Refund updatedRefund = refundRepository.findByUuid(refund.getUuid()).orElseThrow();

        assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.ROLLBACK_FAILED);
        assertThat(updatedRefund.getRefundStatus()).isEqualTo(RefundStatus.CANCELED);

        List<PaymentHistory> histories = paymentHistoryRepository.findByPaymentId(updatedPayment.getId());
        assertThat(histories).extracting(PaymentHistory::getType)
                .contains(PaymentHistoryType.FULL_REFUND_SUCCESS, PaymentHistoryType.FULL_REFUND_FAILED);
    }

    @Test
    @DisplayName("deposit.refund.failed 이벤트에서 부분 환불이 ROLLBACK_FAILED로 전환되며 PARTIAL_REFUND_FAILED 이력이 남는지 검증")
    void sagaFailureRecordsPartialRefundFailedHistory() {
        // given: 부분 환불 요청 준비
        Payment payment = createDonePayment(
                20000L,
                15000L,
                5000L,
                PaymentType.MIXED,
                "pg-key-saga-partial-fail-1",
                "toss-order-saga-partial-fail-1");

        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .paymentId(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .refundAmount(7000L)
                .reason("customer_partial_cancel")
                .build();

        // when: 환불 요청 실행 후 deposit.refund.failed 이벤트 전달
        refundPaymentUseCase.execute(request, "idem-saga-partial-fail-1");

        Refund refund = refundRepository.findByIdempotencyKey("idem-saga-partial-fail-1").orElseThrow();
        Payment afterRequest = paymentRepository.findByUuid(payment.getUuid()).orElseThrow();
        assertThat(afterRequest.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIAL_CANCELED);

        paymentEventListener.handle(new DepositRefundFailedEvent(
                refund.getUuid(),
                payment.getOrderUuid(),
                payment.getUuid(),
                payment.getUserUuid(),
                refund.getRefundDepositTotal(),
                DepositFailureCode.PERSISTENCE_ERROR,
                true,
                "forced partial failure",
                LocalDateTime.now()));

        // then: payment는 rollback-failed, refund는 canceled, 이력은 PARTIAL_REFUND_FAILED 포함
        Payment updatedPayment = paymentRepository.findByUuid(payment.getUuid()).orElseThrow();
        Refund updatedRefund = refundRepository.findByUuid(refund.getUuid()).orElseThrow();

        assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.ROLLBACK_FAILED);
        assertThat(updatedRefund.getRefundStatus()).isEqualTo(RefundStatus.CANCELED);

        List<PaymentHistory> histories = paymentHistoryRepository.findByPaymentId(updatedPayment.getId());
        assertThat(histories).extracting(PaymentHistory::getType)
                .contains(PaymentHistoryType.PARTIAL_REFUND_SUCCESS, PaymentHistoryType.PARTIAL_REFUND_FAILED);
    }

    private Payment createDonePayment(Long amount, Long depositAmount, Long pgAmount,
                                      PaymentType paymentType, String pgPaymentKey, String tossOrderId) {
        Payment payment = Payment.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                amount,
                depositAmount,
                pgAmount,
                0L,
                paymentType,
                tossOrderId);
        payment.approve(pgPaymentKey);
        return paymentRepository.save(payment);
    }
}