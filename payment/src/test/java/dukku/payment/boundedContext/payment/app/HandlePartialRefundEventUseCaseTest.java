package dukku.payment.boundedContext.payment.app;

import dukku.common.shared.order.event.PartialRefundRequestedEvent;
import dukku.common.shared.payment.dto.PaymentRefundRequest;
import dukku.common.shared.payment.exception.InvalidRefundAmountException;
import dukku.common.shared.payment.type.PaymentType;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.payment.boundedContext.payment.entity.PaymentOrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandlePartialRefundEventUseCaseTest {

    @Mock
    private PaymentSupport support;

    @Mock
    private RefundPaymentUseCase refundPaymentUseCase;

    @InjectMocks
    private HandlePartialRefundEventUseCase useCase;

    @Test
    @DisplayName("부분 환불 이벤트 금액과 상관없이 결제 스냅샷 기준으로 환불 금액을 재계산한다")
    void recalculatesRefundAmountFromPaymentSnapshot() {
        // given
        UUID orderUuid = UUID.randomUUID();
        UUID returnRequestUuid = UUID.randomUUID();
        UUID orderItemUuid = UUID.randomUUID();
        UUID sellerUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();

        Payment payment = Payment.create(
                orderUuid,
                userUuid,
                10_000L,
                0L,
                10_000L,
                2_000L,
                PaymentType.MIXED,
                "toss-order-1");
        payment.prePersist();
        payment.approve("pg-key-1");
        ReflectionTestUtils.setField(payment, "id", 1);

        PaymentOrderItem paymentOrderItem = PaymentOrderItem.create(
                payment,
                orderUuid,
                orderItemUuid,
                UUID.randomUUID(),
                "item-1",
                10_000L,
                2_000L,
                sellerUuid,
                0L);
        ReflectionTestUtils.setField(paymentOrderItem, "id", 101);

        PartialRefundRequestedEvent event = new PartialRefundRequestedEvent(
                returnRequestUuid,
                orderUuid,
                userUuid,
                List.of(new PartialRefundRequestedEvent.RefundItemInfo(orderItemUuid, 10_000)));

        when(support.findPaymentsByOrderUuid(orderUuid)).thenReturn(List.of(payment));
        when(support.findPaymentOrderItem(1, orderItemUuid)).thenReturn(Optional.of(paymentOrderItem));
        when(support.getRefundedAmountByPaymentOrderItem(101)).thenReturn(1_000L);

        // when
        useCase.execute(event);

        // then
        ArgumentCaptor<PaymentRefundRequest> requestCaptor = ArgumentCaptor.forClass(PaymentRefundRequest.class);
        verify(refundPaymentUseCase).execute(requestCaptor.capture(), anyString());

        PaymentRefundRequest request = requestCaptor.getValue();
        assertThat(request.getPaymentUuid()).isEqualTo(payment.getUuid());
        assertThat(request.getOrderUuid()).isEqualTo(orderUuid);
        assertThat(request.getRefundAmount()).isEqualTo(7_000L);
        assertThat(request.getItems()).hasSize(1);
        assertThat(request.getItems().get(0).getOrderItemUuid()).isEqualTo(orderItemUuid);
        assertThat(request.getItems().get(0).getRefundAmount()).isEqualTo(7_000L);
    }

    @Test
    @DisplayName("해당 아이템의 환불 가능 금액이 없으면 부분 환불 처리에 실패한다")
    void throwsWhenNoRefundableAmountLeft() {
        // given
        UUID orderUuid = UUID.randomUUID();
        UUID returnRequestUuid = UUID.randomUUID();
        UUID orderItemUuid = UUID.randomUUID();
        UUID sellerUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();

        Payment payment = Payment.create(
                orderUuid,
                userUuid,
                10_000L,
                0L,
                10_000L,
                2_000L,
                PaymentType.MIXED,
                "toss-order-2");
        payment.prePersist();
        payment.approve("pg-key-2");
        ReflectionTestUtils.setField(payment, "id", 2);

        PaymentOrderItem paymentOrderItem = PaymentOrderItem.create(
                payment,
                orderUuid,
                orderItemUuid,
                UUID.randomUUID(),
                "item-2",
                10_000L,
                2_000L,
                sellerUuid,
                0L);
        ReflectionTestUtils.setField(paymentOrderItem, "id", 202);

        PartialRefundRequestedEvent event = new PartialRefundRequestedEvent(
                returnRequestUuid,
                orderUuid,
                userUuid,
                List.of(new PartialRefundRequestedEvent.RefundItemInfo(orderItemUuid, 1_000)));

        when(support.findPaymentsByOrderUuid(orderUuid)).thenReturn(List.of(payment));
        when(support.findPaymentOrderItem(2, orderItemUuid)).thenReturn(Optional.of(paymentOrderItem));
        when(support.getRefundedAmountByPaymentOrderItem(202)).thenReturn(8_000L);

        // when/then
        assertThatThrownBy(() -> useCase.execute(event))
                .isInstanceOf(InvalidRefundAmountException.class);
        verify(refundPaymentUseCase, never()).execute(any(), anyString());
    }
}
