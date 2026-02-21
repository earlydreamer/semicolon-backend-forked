package dukku.order.boundedContext.order.app;

import dukku.common.global.UserUtil;
import dukku.common.shared.order.exception.OrderAccessDeniedException;
import dukku.order.boundedContext.order.entity.OrderItem;
import dukku.order.boundedContext.order.out.OrderItemRepository;
import dukku.common.shared.order.dto.DeliveryInfoRequest;
import dukku.common.shared.order.exception.OrderItemNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateOrderItemDeliveryInfoUseCase {
    private final OrderItemRepository orderItemRepository;

    public void execute(UUID orderItemUuid, DeliveryInfoRequest request) {
        OrderItem orderItem = orderItemRepository.findByUuid(orderItemUuid)
                .orElseThrow(OrderItemNotFoundException::new);

        if (!UserUtil.isAdmin() && !orderItem.getSellerUuid().equals(UserUtil.getUserId())) {
            throw new OrderAccessDeniedException();
        }

        orderItem.updateDeliveryInfo(request);
    }
}
