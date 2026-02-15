package dukku.order.boundedContext.order.app;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateOrderRefundStatusUseCaseHappyPathTest {

    @Mock
    private OrderSupport orderSupport;

    @InjectMocks
    private UpdateOrderRefundStatusUseCase useCase;

    @Test
    @DisplayName("주문 금액만큼 환불되면 환불 누적금이 갱신되고 상태가 CANCELED로 바뀐다")
    void appliesFullRefundToOrderStatus() {
        // given: PAID 상태 주문과 전체 환불 금액이 주어진다.
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

        // when: 환불 상태 업데이트를 실행한다.
        useCase.updateRefund(orderUuid, 12000L);

        // then: 누적 환불액이 갱신되고 주문 상태가 CANCELED로 전이된다.
        verify(orderSupport).findOrderByUuid(orderUuid);
        assertThat(order.getRefundedAmount()).isEqualTo(12000);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }
}
