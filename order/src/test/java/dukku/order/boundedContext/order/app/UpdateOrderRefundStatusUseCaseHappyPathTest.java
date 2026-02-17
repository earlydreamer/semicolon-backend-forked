package dukku.order.boundedContext.order.app;

import dukku.common.shared.order.exception.OrderRefundAmountOutOfRangeException;
import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.entity.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 주문 환불 상태 갱신 UseCase의 정상 동작 테스트
 */
@ExtendWith(MockitoExtension.class)
class UpdateOrderRefundStatusUseCaseHappyPathTest {

    @Mock
    private OrderSupport orderSupport;

    @InjectMocks
    private UpdateOrderRefundStatusUseCase useCase;

    @Test
    @DisplayName("주문 금액만큼 환불되면 환불 누적액이 갱신되고 상태가 CANCELED로 변경된다")
    void appliesFullRefundToOrderStatus() {
        // given: PAID 상태 주문과 전체 환불 금액 준비
        UUID refundUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();

        Order order = Order.builder()
                .uuid(orderUuid)
                .userUuid(UUID.randomUUID())
                .totalAmount(12000)
                .address("seoul")
                .recipient("tester")
                .contactNumber("010-0000-0000")
                .refundedAmount(0)
                .status(OrderStatus.PAID)
                .build();

        when(orderSupport.findOrderByUuid(orderUuid)).thenReturn(order);

        // when: 환불 상태 업데이트 실행
        useCase.updateRefund(refundUuid, orderUuid, 12000L);

        // then: 누적 환불액과 주문 상태가 기대값으로 변경
        verify(orderSupport).findOrderByUuid(orderUuid);
        assertThat(order.getRefundedAmount()).isEqualTo(12000);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("부분 환불 요청이면 환불 누적액이 갱신되고 상태가 PARTIAL_REFUNDED로 변경된다")
    void appliesPartialRefundToOrderStatus() {
        // given: PAID 상태 주문과 부분 환불 금액 준비
        UUID refundUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();

        Order order = Order.builder()
                .uuid(orderUuid)
                .userUuid(UUID.randomUUID())
                .totalAmount(12000)
                .address("seoul")
                .recipient("tester")
                .contactNumber("010-0000-0000")
                .refundedAmount(0)
                .status(OrderStatus.PAID)
                .build();

        when(orderSupport.findOrderByUuid(orderUuid)).thenReturn(order);

        // when: 환불 상태 업데이트 실행
        useCase.updateRefund(refundUuid, orderUuid, 5000L);

        // then: 상태가 PARTIAL_REFUNDED로 반영되고 누적 환불액이 갱신
        verify(orderSupport).findOrderByUuid(orderUuid);
        assertThat(order.getRefundedAmount()).isEqualTo(5000);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIAL_REFUNDED);
    }

    @Test
    @DisplayName("환불 금액이 int 범위를 넘으면 OrderRefundAmountOutOfRangeException이 발생한다")
    void throwsWhenRefundAmountOutOfIntegerRange() {
        // given: int 범위를 초과하는 환불 금액 준비
        UUID refundUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();

        Order order = Order.builder()
                .uuid(orderUuid)
                .userUuid(UUID.randomUUID())
                .totalAmount(12000)
                .address("seoul")
                .recipient("tester")
                .contactNumber("010-0000-0000")
                .refundedAmount(0)
                .status(OrderStatus.PAID)
                .build();

        when(orderSupport.findOrderByUuid(orderUuid)).thenReturn(order);

        // when/then: 예외 발생 확인
        assertThatThrownBy(() -> useCase.updateRefund(refundUuid, orderUuid, (long) Integer.MAX_VALUE + 1))
                .isInstanceOf(OrderRefundAmountOutOfRangeException.class);
    }
}