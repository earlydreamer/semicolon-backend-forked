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
    @DisplayName("전액 환불 시 환불 누적액이 갱신되고 상태가 CANCELED로 변경된다")
    void 전액환불_상태변경() {
        // given: 전액 환불 이벤트와 PAID 상태 주문을 준비한다.
        UUID refundUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        Order order = newPaidOrder(orderUuid, 12_000);

        when(orderSupport.tryMarkRefundCompleted(refundUuid, orderUuid, 12_000L)).thenReturn(true);
        when(orderSupport.findOrderByUuidWithItems(orderUuid)).thenReturn(order);

        // when: 환불 갱신 유스케이스를 실행한다.
        useCase.updateRefund(refundUuid, orderUuid, 12_000L, List.of());

        // then: 환불 누적액이 주문 총액과 같아지고 상태가 CANCELED가 된다.
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

        // when: 환불 갱신 유스케이스를 실행한다.
        useCase.updateRefund(refundUuid, orderUuid, 5_000L, List.of());

        // then: 환불 누적액이 증가하고 상태가 PARTIAL_REFUNDED가 된다.
        verify(orderSupport).findOrderByUuidWithItems(orderUuid);
        assertThat(order.getRefundedAmount()).isEqualTo(5_000);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIAL_REFUNDED);
    }

    @Test
    @DisplayName("환불 금액이 int 범위를 초과하면 OrderRefundAmountOutOfRangeException이 발생한다")
    void 환불금액_범위초과_예외발생() {
        // given: int 범위를 초과하는 환불 금액을 준비한다.
        UUID refundUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        Order order = newPaidOrder(orderUuid, 12_000);
        long overInt = (long) Integer.MAX_VALUE + 1;

        when(orderSupport.tryMarkRefundCompleted(refundUuid, orderUuid, overInt)).thenReturn(true);
        when(orderSupport.findOrderByUuidWithItems(orderUuid)).thenReturn(order);

        // when: 범위를 넘는 환불 금액으로 환불 갱신을 실행한다.
        // then: OrderRefundAmountOutOfRangeException 예외가 발생한다.
        assertThatThrownBy(() -> useCase.updateRefund(refundUuid, orderUuid, overInt, List.of()))
                .isInstanceOf(OrderRefundAmountOutOfRangeException.class);
    }

    @Test
    @DisplayName("환불 이벤트 입력값이 유효하지 않으면 OrderRefundRequestInvalidException이 발생한다")
    void 환불이벤트_입력값검증_실패() {
        // given: refundUuid가 null인 잘못된 환불 이벤트를 준비한다.
        // when: 환불 갱신 유스케이스를 실행한다.
        // then: OrderRefundRequestInvalidException 예외가 발생한다.
        assertThatThrownBy(() -> useCase.updateRefund(null, UUID.randomUUID(), 1_000L, List.of()))
                .isInstanceOf(OrderRefundRequestInvalidException.class);
    }

    @Test
    @DisplayName("이미 처리된 환불 이벤트는 후속 처리를 건너뛴다")
    void 환불이벤트_멱등처리_중복무시() {
        // given: 이미 처리된 환불 이벤트로 표시되도록 준비한다.
        UUID refundUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        when(orderSupport.tryMarkRefundCompleted(refundUuid, orderUuid, 1_000L)).thenReturn(false);

        // when: 동일한 환불 이벤트로 환불 갱신을 다시 호출한다.
        useCase.updateRefund(refundUuid, orderUuid, 1_000L, List.of());

        // then: 후속 저장소 호출이 발생하지 않는다.
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
