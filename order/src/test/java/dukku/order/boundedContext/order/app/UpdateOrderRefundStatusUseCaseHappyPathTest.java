package dukku.order.boundedContext.order.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.order.exception.OrderRefundAmountOutOfRangeException;
import dukku.common.shared.order.exception.OrderRefundRequestInvalidException;
import dukku.common.shared.order.type.OrderItemStatus;
import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.entity.OrderItem;
import dukku.order.boundedContext.order.out.ReturnRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateOrderRefundStatusUseCaseHappyPathTest {

    @Mock
    private OrderSupport orderSupport;

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private UpdateOrderRefundStatusUseCase useCase;

    @Test
    @DisplayName("전액 환불 시 환불 금액이 반영되고 주문 상태는 CANCELED가 된다")
    void fullRefundUpdatesOrderAsCanceled() {
        // given: 전액 환불 이벤트와 PAID 상태 주문을 준비한다.
        UUID refundUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        Order order = newPaidOrder(orderUuid, 12_000);

        when(orderSupport.tryMarkRefundCompleted(refundUuid, orderUuid, 12_000L)).thenReturn(true);
        when(orderSupport.findOrderByUuidWithItems(orderUuid)).thenReturn(order);

        // when: 환불 갱신 유스케이스를 실행한다.
        useCase.updateRefund(refundUuid, orderUuid, 12_000L, List.of());

        // then: 주문 상태/누적 환불액이 갱신되고 판매 복구 이벤트가 발행된다.
        verify(orderSupport).findOrderByUuidWithItems(orderUuid);
        assertThat(order.getRefundedAmount()).isEqualTo(12_000);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("부분 환불 시 주문 상태는 PARTIAL_REFUNDED가 되고 판매 복구 이벤트는 발행되지 않는다")
    void partialRefundDoesNotPublishSaleReleaseEvent() {
        // given: 부분 환불 이벤트와 PAID 상태 주문을 준비한다.
        UUID refundUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        Order order = newPaidOrder(orderUuid, 12_000);

        when(orderSupport.tryMarkRefundCompleted(refundUuid, orderUuid, 5_000L)).thenReturn(true);
        when(orderSupport.findOrderByUuidWithItems(orderUuid)).thenReturn(order);

        // when: 환불 갱신 유스케이스를 실행한다.
        useCase.updateRefund(refundUuid, orderUuid, 5_000L, List.of());

        // then: 주문 상태/누적 환불액이 부분 환불 기준으로 갱신된다.
        verify(orderSupport).findOrderByUuidWithItems(orderUuid);
        assertThat(order.getRefundedAmount()).isEqualTo(5_000);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIAL_REFUNDED);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("환불 금액이 int 범위를 넘으면 OrderRefundAmountOutOfRangeException이 발생한다")
    void refundAmountOverIntThrowsException() {
        // given: int 범위를 초과하는 환불 금액을 준비한다.
        UUID refundUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        Order order = newPaidOrder(orderUuid, 12_000);
        long overInt = (long) Integer.MAX_VALUE + 1;

        when(orderSupport.tryMarkRefundCompleted(refundUuid, orderUuid, overInt)).thenReturn(true);
        when(orderSupport.findOrderByUuidWithItems(orderUuid)).thenReturn(order);

        // when/then: 범위를 넘는 환불 금액으로 환불 갱신 시 예외가 발생한다.
        assertThatThrownBy(() -> useCase.updateRefund(refundUuid, orderUuid, overInt, List.of()))
                .isInstanceOf(OrderRefundAmountOutOfRangeException.class);
    }

    @Test
    @DisplayName("환불 이벤트 입력값이 유효하지 않으면 OrderRefundRequestInvalidException이 발생한다")
    void invalidInputThrowsException() {
        assertThatThrownBy(() -> useCase.updateRefund(null, UUID.randomUUID(), 1_000L, List.of()))
                .isInstanceOf(OrderRefundRequestInvalidException.class);
    }

    @Test
    @DisplayName("이미 처리된 환불 이벤트는 무시한다")
    void duplicateRefundEventIsIgnored() {
        // given: 이미 처리된 환불 이벤트로 표시되도록 준비한다.
        UUID refundUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();

        when(orderSupport.tryMarkRefundCompleted(refundUuid, orderUuid, 1_000L)).thenReturn(false);

        // when: 동일한 환불 이벤트로 환불 갱신을 다시 호출한다.
        useCase.updateRefund(refundUuid, orderUuid, 1_000L, List.of());

        // then: 후속 저장소/이벤트 발행 호출이 발생하지 않는다.
        verifyNoInteractions(returnRequestRepository);
        verifyNoInteractions(eventPublisher);
    }

    private Order newPaidOrder(UUID orderUuid, int totalAmount) {
        Order order = Order.builder()
                .uuid(orderUuid)
                .userUuid(UUID.randomUUID())
                .totalAmount(totalAmount)
                .address("seoul")
                .recipient("tester")
                .contactNumber("010-0000-0000")
                .refundedAmount(0)
                .status(OrderStatus.PAID)
                .build();

        order.addOrderItem(OrderItem.builder()
                .productUuid(UUID.randomUUID())
                .sellerUuid(UUID.randomUUID())
                .productName("test-product")
                .productPrice(totalAmount)
                .status(OrderItemStatus.PAYMENT_COMPLETED)
                .build());

        return order;
    }
}
