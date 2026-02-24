package dukku.order.boundedContext.order.app;

import dukku.common.global.UserUtil;
import dukku.common.shared.order.exception.OrderAccessDeniedException;
import dukku.order.boundedContext.order.entity.Order;
import dukku.common.shared.order.dto.OrderUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateShippingInfoUseCase {
    private final OrderSupport orderSupport;

    @Transactional
    public void execute(UUID orderUuid, OrderUpdateRequest.ShippingInfo req) {
        Order order = orderSupport.findOrderByUuidWithItems(orderUuid);

        if (!UserUtil.isAdmin() && !order.getUserUuid().equals(UserUtil.getUserId())) {
            throw new OrderAccessDeniedException();
        }

        order.updateOrderForUser(req.getAddress(), req.getRecipient(), req.getContactNumber());
    }
}
