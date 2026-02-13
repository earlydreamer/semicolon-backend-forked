package dukku.payment.boundedContext.payment.app;

import dukku.common.global.event.DomainEvent;
import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.payment.dto.PaymentRefundRequest;
import dukku.common.shared.payment.dto.PaymentRefundResponse;
import dukku.common.shared.payment.event.RefundCompletedEvent;
import dukku.common.shared.payment.type.PaymentHistoryType;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.common.shared.payment.type.PaymentType;
import dukku.common.shared.payment.type.RefundStatus;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.payment.boundedContext.payment.entity.Refund;
import dukku.payment.boundedContext.payment.out.TossPaymentClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundPaymentUseCaseHappyPathTest {

    @Mock
    private PaymentSupport support;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private TossPaymentClient tossClient;

    @InjectMocks
    private RefundPaymentUseCase useCase;

    @Test
    @DisplayName("전체 환불 성공 시 결제 상태/환불 엔티티/이벤트가 일관되게 반영된다")
    void executesFullRefundSuccessfully() {
        // given: DONE 결제와 전체 환불 요청을 준비하고 PG 취소 응답을 성공으로 설정한다.
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();

        Payment payment = Payment.create(
                orderUuid,
                userUuid,
                10000L,
                0L,
                10000L,
                0L,
                PaymentType.NORMAL,
                "toss-order-100");
        payment.prePersist();
        payment.approve("pg-key-100");

        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .paymentId(payment.getUuid())
                .orderUuid(orderUuid)
                .refundAmount(10000L)
                .reason("customer_cancel_request")
                .build();

        when(support.findRefundByIdempotencyKey("idem-full-100")).thenReturn(Optional.empty());
        when(support.findPaymentByUuid(payment.getUuid())).thenReturn(payment);
        when(support.saveRefund(any(Refund.class))).thenAnswer(invocation -> {
            Refund refund = invocation.getArgument(0);
            refund.prePersist();
            return refund;
        });

        Map<String, Object> cancelResponse = new HashMap<>();
        cancelResponse.put("statusCode", 200);
        when(tossClient.cancel(eq("pg-key-100"), anyMap())).thenReturn(cancelResponse);

        // when: 환불 유스케이스를 실행한다.
        PaymentRefundResponse response = useCase.execute(request, "idem-full-100");

        // then: 결제/환불 상태, 이력 생성, 이벤트 payload가 전체 환불 기준으로 반영된다.
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo("REFUND_COMPLETED");
        assertThat(response.getData().getOrderUuid()).isEqualTo(orderUuid);
        assertThat(response.getData().getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(response.getData().getAmounts().getRequestedRefundAmount()).isEqualTo(10000L);
        assertThat(response.getData().getAmounts().getDepositRefundAmount()).isEqualTo(0L);
        assertThat(response.getData().getAmounts().getPgRefundAmount()).isEqualTo(10000L);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.getRefundTotal()).isEqualTo(10000L);
        assertThat(payment.getAmountPg()).isEqualTo(0L);
        assertThat(payment.getPaymentDeposit()).isEqualTo(0L);

        ArgumentCaptor<Map<String, Object>> cancelBodyCaptor = ArgumentCaptor.forClass((Class) Map.class);
        verify(tossClient).cancel(eq("pg-key-100"), cancelBodyCaptor.capture());
        assertThat(cancelBodyCaptor.getValue()).containsEntry("cancelReason", "customer_cancel_request");
        assertThat(cancelBodyCaptor.getValue()).containsEntry("cancelAmount", 10000L);

        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
        verify(support).saveRefund(refundCaptor.capture());
        Refund savedRefund = refundCaptor.getValue();
        assertThat(savedRefund.getRefundAmountTotal()).isEqualTo(10000L);
        assertThat(savedRefund.getRefundDepositTotal()).isEqualTo(0L);
        assertThat(savedRefund.getRefundStatus()).isEqualTo(RefundStatus.COMPLETED);

        verify(support).savePayment(payment);
        verify(support).createHistory(
                eq(payment),
                eq(PaymentHistoryType.FULL_REFUND_SUCCESS),
                eq(PaymentStatus.DONE),
                eq(10000L),
                eq(0L));

        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(RefundCompletedEvent.class);

        RefundCompletedEvent event = (RefundCompletedEvent) eventCaptor.getValue();
        assertThat(event.orderUuid()).isEqualTo(orderUuid);
        assertThat(event.paymentId()).isEqualTo(payment.getUuid());
        assertThat(event.refundAmount()).isEqualTo(10000L);
        assertThat(event.refundDepositAmount()).isEqualTo(0L);
        assertThat(event.userUuid()).isEqualTo(userUuid);
    }
}
