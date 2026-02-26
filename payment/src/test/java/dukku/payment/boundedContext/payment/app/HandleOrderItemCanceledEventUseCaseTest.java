package dukku.payment.boundedContext.payment.app;

import dukku.common.shared.order.event.OrderItemCanceledEvent;
import dukku.common.shared.payment.dto.PaymentRefundRequest;
import dukku.common.shared.payment.type.PaymentHistoryType;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.common.shared.payment.type.PaymentType;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.payment.boundedContext.payment.entity.PaymentOrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleOrderItemCanceledEventUseCaseTest {

    @Mock
    private PaymentSupport support;

    @Mock
    private RefundPaymentUseCase refundPaymentUseCase;

    @InjectMocks
    private HandleOrderItemCanceledEventUseCase useCase;

    @Test
    @DisplayName("PENDING 결제에서 주문상품 취소 이벤트를 받으면 결제를 즉시 CANCELED로 전환한다")
    void cancelPendingPaymentDirectly() {
        UUID orderUuid = UUID.randomUUID();
        UUID orderItemUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();

        Payment payment = Payment.create(orderUuid, userUuid, 10_000L,
                0L, 10_000L, 0L, PaymentType.NORMAL, "order-1");
        PaymentOrderItem item = PaymentOrderItem.create(payment, orderUuid, orderItemUuid,
                UUID.randomUUID(), "item", 10_000L, 0L, UUID.randomUUID(), 0L);
        payment.addItem(item);

        when(support.findPaymentByOrderItemUuid(orderItemUuid)).thenReturn(Optional.of(payment));

        useCase.execute(new OrderItemCanceledEvent(orderItemUuid));

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
        verify(support).savePayment(payment);
        verify(support).createHistory(payment, PaymentHistoryType.ORDER_CANCEL_SUCCESS,
                PaymentStatus.PENDING, 10_000L, 0L);
        verify(refundPaymentUseCase, never()).execute(any(PaymentRefundRequest.class), any(String.class));
    }

    @Test
    @DisplayName("DONE 결제에서 주문상품 취소 이벤트를 받으면 환불 플로우를 호출한다")
    void triggerRefundForDonePayment() {
        UUID orderUuid = UUID.randomUUID();
        UUID orderItemUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();

        Payment payment = Payment.create(orderUuid, userUuid, 10_000L,
                0L, 10_000L, 0L, PaymentType.NORMAL, "order-2");
        payment.approve("pg-key");

        PaymentOrderItem item = PaymentOrderItem.create(payment, orderUuid, orderItemUuid,
                UUID.randomUUID(), "item", 10_000L, 0L, UUID.randomUUID(), 0L);
        payment.addItem(item);

        when(support.findPaymentByOrderItemUuid(orderItemUuid)).thenReturn(Optional.of(payment));

        useCase.execute(new OrderItemCanceledEvent(orderItemUuid));

        verify(refundPaymentUseCase).execute(any(PaymentRefundRequest.class), eq("CANCEL_" + orderItemUuid));
        verify(support, never()).savePayment(any(Payment.class));
    }
}