package dukku.order.boundedContext.order.app;

import dukku.common.shared.order.type.OrderItemStatus;
import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateOrderStatusForAdminUseCase {
    private final OrderSupport orderSupport;

    @Transactional
    public void execute(UUID orderUuid, OrderStatus status) {
        Order order = orderSupport.findOrderByUuidWithItems(orderUuid);
        order.updateOrderStatus(status);

        OrderItemStatus targetItemStatus = mapToOrderItemStatus(status);
        if (targetItemStatus == null) {
            return;
        }
        order.getOrderItems().forEach(item -> item.forceUpdateOrderStatusForAdmin(targetItemStatus));
    }

    private OrderItemStatus mapToOrderItemStatus(OrderStatus status) {
        return switch (status) {
            case PAID -> OrderItemStatus.PAYMENT_COMPLETED;
            case CANCELED, PAYMENT_FAILED -> OrderItemStatus.CANCELED;
            default -> null;
        };
    }
}
