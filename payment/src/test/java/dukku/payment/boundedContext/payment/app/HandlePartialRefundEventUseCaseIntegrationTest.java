package dukku.payment.boundedContext.payment.app;

import dukku.common.shared.order.event.PartialRefundRequestedEvent;
import dukku.common.shared.payment.dto.PaymentRefundRequest;
import dukku.common.shared.payment.exception.InvalidRefundAmountException;
import dukku.common.shared.payment.type.PaymentType;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.payment.boundedContext.payment.entity.PaymentOrderItem;
import dukku.payment.boundedContext.payment.entity.Refund;
import dukku.payment.boundedContext.payment.entity.RefundItem;
import dukku.payment.boundedContext.payment.out.PaymentRepository;
import dukku.payment.boundedContext.payment.out.RefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:handle_partial_refund_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
        "crypto.key=dGhpcy1rZXktaXMtdGVzdC1rZXktY3J5cHRvLTAxMjM=",
        "toss.api.secret-key=test_toss_secret_key"
})
@Transactional
class HandlePartialRefundEventUseCaseIntegrationTest {

    @Autowired
    private HandlePartialRefundEventUseCase useCase;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @MockitoBean
    private RefundPaymentUseCase refundPaymentUseCase;

    @BeforeEach
    void cleanUp() {
        refundRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("부분환불 이벤트 금액보다 결제 스냅샷 재계산 금액이 우선 적용된다")
    void appliesSnapshotCalculatedRefundAmount() {
        UUID orderUuid = UUID.randomUUID();
        UUID orderItemUuid = UUID.randomUUID();
        UUID returnRequestUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();

        Payment payment = createPaymentWithSingleItem(orderUuid, orderItemUuid, userUuid, 10_000L, 2_000L);
        PaymentOrderItem paymentOrderItem = payment.getItems().get(0);

        // 기존 환불 1,000원 반영: 환불 가능 금액은 7,000원
        createPreviousRefund(payment, paymentOrderItem, 1_000L);

        PartialRefundRequestedEvent event = new PartialRefundRequestedEvent(
                returnRequestUuid,
                orderUuid,
                userUuid,
                List.of(new PartialRefundRequestedEvent.RefundItemInfo(orderItemUuid, 10_000)));

        useCase.execute(event);

        ArgumentCaptor<PaymentRefundRequest> requestCaptor = ArgumentCaptor.forClass(PaymentRefundRequest.class);
        verify(refundPaymentUseCase).execute(requestCaptor.capture(), eq(returnRequestUuid.toString()));

        PaymentRefundRequest request = requestCaptor.getValue();
        assertThat(request.getRefundAmount()).isEqualTo(7_000L);
        assertThat(request.getItems()).hasSize(1);
        assertThat(request.getItems().get(0).getOrderItemUuid()).isEqualTo(orderItemUuid);
        assertThat(request.getItems().get(0).getRefundAmount()).isEqualTo(7_000L);
    }

    @Test
    @DisplayName("환불 가능 금액이 0원이면 부분환불 요청을 거절한다")
    void rejectsWhenRefundableAmountIsZero() {
        UUID orderUuid = UUID.randomUUID();
        UUID orderItemUuid = UUID.randomUUID();
        UUID returnRequestUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();

        Payment payment = createPaymentWithSingleItem(orderUuid, orderItemUuid, userUuid, 10_000L, 2_000L);
        PaymentOrderItem paymentOrderItem = payment.getItems().get(0);

        // 순결제금액 8,000원을 이미 모두 환불
        createPreviousRefund(payment, paymentOrderItem, 8_000L);

        PartialRefundRequestedEvent event = new PartialRefundRequestedEvent(
                returnRequestUuid,
                orderUuid,
                userUuid,
                List.of(new PartialRefundRequestedEvent.RefundItemInfo(orderItemUuid, 1_000)));

        assertThatThrownBy(() -> useCase.execute(event))
                .isInstanceOf(InvalidRefundAmountException.class);
        verify(refundPaymentUseCase, never()).execute(any(), anyString());
    }

    private Payment createPaymentWithSingleItem(UUID orderUuid, UUID orderItemUuid, UUID userUuid,
            long itemPrice, long itemCoupon) {
        Payment payment = Payment.create(
                orderUuid,
                userUuid,
                itemPrice - itemCoupon,
                0L,
                itemPrice - itemCoupon,
                itemCoupon,
                PaymentType.MIXED,
                "toss-order-" + UUID.randomUUID());
        payment.prePersist();
        payment.approve("pg-key-" + UUID.randomUUID());

        payment.addItem(PaymentOrderItem.create(
                payment,
                orderUuid,
                orderItemUuid,
                1,
                "item",
                itemPrice,
                itemCoupon,
                UUID.randomUUID(),
                0L));

        Payment savedPayment = paymentRepository.save(payment);
        paymentRepository.flush();
        return savedPayment;
    }

    private void createPreviousRefund(Payment payment, PaymentOrderItem paymentOrderItem, long refundedAmount) {
        Refund refund = payment.createRefund(refundedAmount, 0L, "idem-prev-" + UUID.randomUUID());
        refund.complete();
        refund.addRefundItem(RefundItem.create(
                refund,
                paymentOrderItem,
                refundedAmount,
                0L,
                refundedAmount));
        refundRepository.save(refund);
        refundRepository.flush();
    }
}
