package dukku.order.boundedContext.order.app;

import dukku.common.shared.order.exception.OrderRefundAmountOutOfRangeException;
import dukku.common.shared.order.exception.OrderRefundRequestInvalidException;
import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.entity.Order;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 주문 환불 상태 갱신 유스케이스 정상/예외 경로 테스트.
 */
@ExtendWith(MockitoExtension.class)
class UpdateOrderRefundStatusUseCaseHappyPathTest {

    @Mock
    private OrderSupport orderSupport;

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @InjectMocks
    private UpdateOrderRefundStatusUseCase useCase;

    @Test
    @DisplayName("주문 금액만큼 환불되면 환불 누적액이 갱신되고 상태가 CANCELED로 변경된다")
    void 주문_전액환불_상태변경() {
        // given
        UUID refundUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        Order order = newPaidOrder(orderUuid, 12_000);

        when(orderSupport.tryMarkRefundCompleted(refundUuid, orderUuid, 12_000L)).thenReturn(true);
        when(orderSupport.findOrderByUuidWithItems(orderUuid)).thenReturn(order);

        // when
        useCase.updateRefund(refundUuid, orderUuid, 12_000L, List.of());

        // then
        verify(orderSupport).findOrderByUuidWithItems(orderUuid);
        assertThat(order.getRefundedAmount()).isEqualTo(12_000);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("부분 환불이면 환불 누적액이 갱신되고 상태가 PARTIAL_REFUNDED로 변경된다")
    void 주문_부분환불_상태변경() {
        // given
        UUID refundUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        Order order = newPaidOrder(orderUuid, 12_000);

        when(orderSupport.tryMarkRefundCompleted(refundUuid, orderUuid, 5_000L)).thenReturn(true);
        when(orderSupport.findOrderByUuidWithItems(orderUuid)).thenReturn(order);

        // when
        useCase.updateRefund(refundUuid, orderUuid, 5_000L, List.of());

        // then
        verify(orderSupport).findOrderByUuidWithItems(orderUuid);
        assertThat(order.getRefundedAmount()).isEqualTo(5_000);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIAL_REFUNDED);
    }

    @Test
    @DisplayName("환불 금액이 int 범위를 넘으면 OrderRefundAmountOutOfRangeException이 발생한다")
    void 환불금액_범위초과_예외발생() {
        // given
        UUID refundUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        Order order = newPaidOrder(orderUuid, 12_000);
        long overInt = (long) Integer.MAX_VALUE + 1;

        when(orderSupport.tryMarkRefundCompleted(refundUuid, orderUuid, overInt)).thenReturn(true);
        when(orderSupport.findOrderByUuidWithItems(orderUuid)).thenReturn(order);

        // when/then
        assertThatThrownBy(() -> useCase.updateRefund(refundUuid, orderUuid, overInt, List.of()))
                .isInstanceOf(OrderRefundAmountOutOfRangeException.class);
    }

    @Test
    @DisplayName("환불 이벤트 입력값이 비어 있으면 OrderRefundRequestInvalidException이 발생한다")
    void 환불이벤트_입력값_검증실패() {
        assertThatThrownBy(() -> useCase.updateRefund(null, UUID.randomUUID(), 1_000L, List.of()))
                .isInstanceOf(OrderRefundRequestInvalidException.class);
    }

    @Test
    @DisplayName("이미 처리된 환불 이벤트면 후속 처리를 하지 않는다")
    void 환불이벤트_멱등처리_중복무시() {
        // given
        UUID refundUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        when(orderSupport.tryMarkRefundCompleted(refundUuid, orderUuid, 1_000L)).thenReturn(false);

        // when
        useCase.updateRefund(refundUuid, orderUuid, 1_000L, List.of());

        // then
        verifyNoInteractions(returnRequestRepository);
    }

    private Order newPaidOrder(UUID orderUuid, int totalAmount) {
        return Order.builder()
                .uuid(orderUuid)
                .userUuid(UUID.randomUUID())
                .totalAmount(totalAmount)
                .address("seoul")
                .recipient("tester")
                .contactNumber("010-0000-0000")
                .refundedAmount(0)
                .status(OrderStatus.PAID)
                .build();
    }
}
