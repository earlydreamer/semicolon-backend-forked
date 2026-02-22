package dukku.order.boundedContext.order.app;

import dukku.common.shared.order.dto.OrderListResponse;
import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindOrderListByOrderStatusUseCase {
    private final OrderRepository orderRepository;

    public List<OrderListResponse> execute(UUID userUuid, OrderStatus status, int limit) {
        List<Order> orders = orderRepository.findRecentByUserUuidAndStatusWithItems(userUuid, status, limit);

        return orders.stream()
                .map(Order::fromOrderListResponse)
                .toList();
    }
}

