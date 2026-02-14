package dukku.payment.boundedContext.payment.app;

import dukku.common.global.event.DomainEvent;
import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.payment.dto.PaymentRefundRequest;
import dukku.common.shared.payment.dto.PaymentRefundResponse;
import dukku.common.shared.payment.event.RefundCompletedEvent;
import dukku.common.shared.payment.exception.InvalidRefundAmountException;
import dukku.common.shared.payment.type.PaymentHistoryType;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.common.shared.payment.type.PaymentType;
import dukku.common.shared.payment.type.RefundStatus;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.payment.boundedContext.payment.entity.PaymentHistory;
import dukku.payment.boundedContext.payment.entity.PaymentOrderItem;
import dukku.payment.boundedContext.payment.entity.Refund;
import dukku.payment.boundedContext.payment.out.PaymentHistoryRepository;
import dukku.payment.boundedContext.payment.out.PaymentOrderItemRepository;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_refund_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
class PaymentIntegrationTest {

    @Autowired
    private RefundPaymentUseCase refundPaymentUseCase;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    @Autowired
    private PaymentOrderItemRepository paymentOrderItemRepository;

    @Autowired
    private RefundItemRepository refundItemRepository;

    @MockitoBean
    private TossPaymentClient tossPaymentClient;

    @MockitoBean
    private EventPublisher eventPublisher;

    @BeforeEach
    void cleanUp() {
        paymentHistoryRepository.deleteAll();
        refundItemRepository.deleteAll();
        refundRepository.deleteAll();
        paymentOrderItemRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("PG 단독 결제 전체 환불 시 PG 취소/DB 상태/이벤트 발행이 모두 반영된다")
    void fullRefundPgOnlyFlow() {
        // given: PG 단독 결제가 DONE 상태이고 전체 환불 요청을 준비한다.
        Payment payment = createDonePayment(
                10000L,
                0L,
                10000L,
                PaymentType.NORMAL,
                "pg-key-full-1",
                "toss-order-full-1");

        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .paymentId(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .refundAmount(10000L)
                .reason("customer_cancel_request")
                .build();

        when(tossPaymentClient.cancel(eq("pg-key-full-1"), anyMap()))
                .thenReturn(Map.of("statusCode", 200));

        // when: 환불 유스케이스를 실행한다.
        PaymentRefundResponse response = refundPaymentUseCase.execute(request, "idem-full-pg-only");

        // then: PG 취소 요청/결제-환불-이력 저장/이벤트 발행이 전체 환불 값으로 반영된다.
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo("REFUND_COMPLETED");
        assertThat(response.getData().getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(response.getData().getAmounts().getRequestedRefundAmount()).isEqualTo(10000L);
        assertThat(response.getData().getAmounts().getDepositRefundAmount()).isEqualTo(0L);
        assertThat(response.getData().getAmounts().getPgRefundAmount()).isEqualTo(10000L);

        ArgumentCaptor<Map<String, Object>> cancelCaptor = ArgumentCaptor.forClass((Class) Map.class);
        verify(tossPaymentClient).cancel(eq("pg-key-full-1"), cancelCaptor.capture());
        assertThat(cancelCaptor.getValue()).containsEntry("cancelReason", "customer_cancel_request");
        assertThat(cancelCaptor.getValue()).containsEntry("cancelAmount", 10000L);

        Payment updatedPayment = paymentRepository.findByUuid(payment.getUuid()).orElseThrow();
        assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(updatedPayment.getAmountPg()).isEqualTo(0L);
        assertThat(updatedPayment.getPaymentDeposit()).isEqualTo(0L);
        assertThat(updatedPayment.getRefundTotal()).isEqualTo(10000L);

        Refund refund = refundRepository.findByIdempotencyKey("idem-full-pg-only").orElseThrow();
        assertThat(refund.getRefundAmountTotal()).isEqualTo(10000L);
        assertThat(refund.getRefundDepositTotal()).isEqualTo(0L);
        assertThat(refund.getRefundStatus()).isEqualTo(RefundStatus.COMPLETED);

        List<PaymentHistory> histories = paymentHistoryRepository.findByPaymentId(updatedPayment.getId());
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getType()).isEqualTo(PaymentHistoryType.FULL_REFUND_SUCCESS);

        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(RefundCompletedEvent.class);

        RefundCompletedEvent event = (RefundCompletedEvent) eventCaptor.getValue();
        assertThat(event.orderUuid()).isEqualTo(payment.getOrderUuid());
        assertThat(event.paymentId()).isEqualTo(payment.getUuid());
        assertThat(event.refundAmount()).isEqualTo(10000L);
        assertThat(event.refundDepositAmount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("혼합 결제 전체 환불 시 PG는 PG 금액만 취소하고 예치금 환불은 이벤트에 포함된다")
    void fullRefundMixedFlow() {
        // given: MIXED 결제가 DONE 상태이고 전체 환불 요청을 준비한다.
        Payment payment = createDonePayment(
                20000L,
                15000L,
                5000L,
                PaymentType.MIXED,
                "pg-key-full-2",
                "toss-order-full-2");

        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .paymentId(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .refundAmount(20000L)
                .reason("customer_cancel_request")
                .build();

        when(tossPaymentClient.cancel(eq("pg-key-full-2"), anyMap()))
                .thenReturn(Map.of("statusCode", 200));

        // when: 환불 유스케이스를 실행한다.
        PaymentRefundResponse response = refundPaymentUseCase.execute(request, "idem-full-mixed");

        // then: PG 취소 금액은 PG 몫으로 계산되고 예치금 환불 금액도 이벤트에 반영된다.
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(response.getData().getAmounts().getRequestedRefundAmount()).isEqualTo(20000L);
        assertThat(response.getData().getAmounts().getDepositRefundAmount()).isEqualTo(15000L);
        assertThat(response.getData().getAmounts().getPgRefundAmount()).isEqualTo(5000L);

        ArgumentCaptor<Map<String, Object>> cancelCaptor = ArgumentCaptor.forClass((Class) Map.class);
        verify(tossPaymentClient).cancel(eq("pg-key-full-2"), cancelCaptor.capture());
        assertThat(cancelCaptor.getValue()).containsEntry("cancelAmount", 5000L);

        Payment updatedPayment = paymentRepository.findByUuid(payment.getUuid()).orElseThrow();
        assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(updatedPayment.getAmountPg()).isEqualTo(0L);
        assertThat(updatedPayment.getPaymentDeposit()).isEqualTo(0L);
        assertThat(updatedPayment.getRefundTotal()).isEqualTo(20000L);

        Refund refund = refundRepository.findByIdempotencyKey("idem-full-mixed").orElseThrow();
        assertThat(refund.getRefundAmountTotal()).isEqualTo(20000L);
        assertThat(refund.getRefundDepositTotal()).isEqualTo(15000L);
        assertThat(refund.getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        List<PaymentHistory> histories = paymentHistoryRepository.findByPaymentId(updatedPayment.getId());
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getType()).isEqualTo(PaymentHistoryType.FULL_REFUND_SUCCESS);

        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        DomainEvent event = eventCaptor.getValue();
        assertThat(event.getTopic()).isEqualTo("payment.refund-requested");
        assertThat(event.getKey()).isEqualTo(payment.getOrderUuid().toString());
    }

    @Test
    @DisplayName("혼합 결제 환불은 요청 이벤트를 먼저 발행한다")
    void mixedRefundStartsSaga() {
        Payment payment = createDonePayment(
                20000L,
                15000L,
                5000L,
                PaymentType.MIXED,
                "pg-key-mixed-requested-1",
                "toss-order-mixed-requested-1");

        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .paymentId(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .refundAmount(20000L)
                .reason("customer_cancel_request")
                .build();

        when(tossPaymentClient.cancel(eq("pg-key-mixed-requested-1"), anyMap()))
                .thenReturn(Map.of("statusCode", 200));

        refundPaymentUseCase.execute(request, "idem-mixed-requested-1");

        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getTopic()).isEqualTo("payment.refund-requested");
    }

    @Test
    @DisplayName("동일 idempotency key로 재요청하면 기존 환불 결과를 재사용하고 PG 취소는 1회만 호출된다")
    void duplicateIdemReusesRefund() {
        // given: 동일 idempotency key로 같은 전체 환불 요청을 2회 수행할 입력을 준비한다.
        Payment payment = createDonePayment(
                10000L,
                0L,
                10000L,
                PaymentType.NORMAL,
                "pg-key-full-3",
                "toss-order-full-3");

        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .paymentId(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .refundAmount(10000L)
                .reason("customer_cancel_request")
                .build();

        when(tossPaymentClient.cancel(eq("pg-key-full-3"), anyMap()))
                .thenReturn(Map.of("statusCode", 200));

        // when: 같은 idempotency key로 환불을 두 번 호출한다.
        PaymentRefundResponse first = refundPaymentUseCase.execute(request, "idem-dup");
        PaymentRefundResponse second = refundPaymentUseCase.execute(request, "idem-dup");

        // then: PG 취소는 한 번만 호출되고 두 번째 응답은 기존 결과를 재사용한다.
        verify(tossPaymentClient, times(1)).cancel(eq("pg-key-full-3"), anyMap());
        assertThat(first.isSuccess()).isTrue();
        assertThat(second.isSuccess()).isTrue();
        assertThat(second.getData().getOrderUuid()).isEqualTo(first.getData().getOrderUuid());
        assertThat(second.getData().getAmounts().getRequestedRefundAmount())
                .isEqualTo(first.getData().getAmounts().getRequestedRefundAmount());
    }

    @Test
    @DisplayName("쿠폰 적용 상품은 순결제금액을 넘겨 환불할 수 없다")
    void couponItemRefundBound() {
        Payment payment = createDonePayment(
                17000L,
                0L,
                17000L,
                PaymentType.NORMAL,
                "pg-key-coupon-bound-1",
                "toss-order-coupon-bound-1");

        UUID couponItem = UUID.randomUUID();
        createOrderItem(payment, couponItem, 10000L, 3000L, 0L);
        createOrderItem(payment, UUID.randomUUID(), 10000L, 0L, 0L);

        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .paymentId(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .refundAmount(8000L)
                .reason("customer_cancel_request")
                .items(List.of(PaymentRefundRequest.RefundItemInfo.builder()
                        .orderItemUuid(couponItem)
                        .refundAmount(8000L)
                        .build()))
                .build();

        when(tossPaymentClient.cancel(eq("pg-key-coupon-bound-1"), anyMap()))
                .thenReturn(Map.of("statusCode", 200));

        assertThatThrownBy(() -> refundPaymentUseCase.execute(request, "idem-coupon-bound-1"))
                .isInstanceOf(InvalidRefundAmountException.class);
    }

    @Test
    @DisplayName("같은 상품 부분환불은 여러 번 누적 반영된다")
    void sameItemTwoPartialFlow() {
        Payment payment = createDonePayment(
                10000L,
                0L,
                10000L,
                PaymentType.NORMAL,
                "pg-key-item-repeat-1",
                "toss-order-item-repeat-1");

        PaymentOrderItem item = createOrderItem(payment, UUID.randomUUID(), 10000L, 0L, 0L);

        PaymentRefundRequest firstReq = PaymentRefundRequest.builder()
                .paymentId(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .refundAmount(4000L)
                .reason("customer_cancel_request")
                .items(List.of(PaymentRefundRequest.RefundItemInfo.builder()
                        .orderItemUuid(item.getOrderItemUuid())
                        .refundAmount(4000L)
                        .build()))
                .build();

        PaymentRefundRequest secondReq = PaymentRefundRequest.builder()
                .paymentId(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .refundAmount(3000L)
                .reason("customer_cancel_request")
                .items(List.of(PaymentRefundRequest.RefundItemInfo.builder()
                        .orderItemUuid(item.getOrderItemUuid())
                        .refundAmount(3000L)
                        .build()))
                .build();

        when(tossPaymentClient.cancel(eq("pg-key-item-repeat-1"), anyMap()))
                .thenReturn(Map.of("statusCode", 200));

        PaymentRefundResponse first = refundPaymentUseCase.execute(firstReq, "idem-item-repeat-1");
        PaymentRefundResponse second = refundPaymentUseCase.execute(secondReq, "idem-item-repeat-2");

        assertThat(first.isSuccess()).isTrue();
        assertThat(second.isSuccess()).isTrue();
        assertThat(refundItemRepository.findByPaymentOrderItemId(item.getId())).hasSize(2);
    }

    @Test
    @DisplayName("예치금 환불 대기 상태는 REFUND_PENDING 응답을 반환한다")
    void pendingRefundCode() {
        Payment payment = createDonePayment(
                20000L,
                15000L,
                5000L,
                PaymentType.MIXED,
                "pg-key-pending-code-1",
                "toss-order-pending-code-1");

        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .paymentId(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .refundAmount(20000L)
                .reason("customer_cancel_request")
                .build();

        when(tossPaymentClient.cancel(eq("pg-key-pending-code-1"), anyMap()))
                .thenReturn(Map.of("statusCode", 200));

        PaymentRefundResponse response = refundPaymentUseCase.execute(request, "idem-pending-code-1");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo("REFUND_PENDING");
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

    private PaymentOrderItem createOrderItem(Payment payment, UUID orderItemUuid,
                                             Long price, Long coupon, Long deposit) {
        return paymentOrderItemRepository.save(PaymentOrderItem.create(
                payment,
                payment.getOrderUuid(),
                orderItemUuid,
                1,
                "item-" + orderItemUuid.toString().substring(0, 8),
                price,
                coupon,
                UUID.randomUUID(),
                deposit));
    }
}
