package dukku.order.boundedContext.order.app;

import dukku.common.global.UserUtil;
import dukku.common.shared.order.exception.OrderAccessDeniedException;
import dukku.order.boundedContext.order.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FindOrderUseCase {
    private final OrderSupport orderSupport;

    @Transactional(readOnly = true)
    public Order execute(UUID orderUuid) {
        Order order = orderSupport.findOrderByUuidWithItems(orderUuid);

        if (!UserUtil.isAdmin() && !order.getUserUuid().equals(UserUtil.getUserId())) {
            throw new OrderAccessDeniedException();
        }

        return order;
    }
}
