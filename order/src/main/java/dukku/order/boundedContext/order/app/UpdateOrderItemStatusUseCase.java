package dukku.order.boundedContext.order.app;

import dukku.common.global.UserUtil;
import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.order.event.OrderItemCanceledEvent;
import dukku.common.shared.order.event.OrderItemConfirmedEvent;
import dukku.common.shared.order.event.OrderProductSaleReleasedEvent;
import dukku.common.shared.order.event.OrderItemRefundRequestedEvent;
import dukku.common.shared.order.exception.OrderAccessDeniedException;
import dukku.common.shared.order.exception.OrderItemActionNotAllowedException;
import dukku.common.shared.order.exception.OrderItemNotFoundException;
import dukku.common.shared.order.type.OrderItemStatus;
import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.entity.OrderItem;
import dukku.order.boundedContext.order.out.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateOrderItemStatusUseCase {
    private final OrderItemRepository orderItemRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void execute(UUID orderItemUuid, OrderItemStatus newStatus) {
        if (newStatus == null) {
            return;
        }

        OrderItem orderItem = orderItemRepository.findByUuid(orderItemUuid)
                .orElseThrow(OrderItemNotFoundException::new);

        checkPermission(orderItem, newStatus);

        // 배송 전 사용자 취소 요청(CANCEL_REQUESTED)은 즉시 취소(CANCELED)로 처리한다.
        OrderItemStatus targetStatus = resolveTargetStatus(orderItem, newStatus);

        orderItem.updateOrderStatus(targetStatus);
        syncOrderStatusIfAllItemsCanceled(orderItem, targetStatus);
        publishEvent(orderItem, targetStatus);

        log.info("Order item status updated. orderItemUuid={}, requestedStatus={}, appliedStatus={}",
                orderItemUuid, newStatus, targetStatus);
    }

    private OrderItemStatus resolveTargetStatus(OrderItem orderItem, OrderItemStatus requestedStatus) {
        if (requestedStatus == OrderItemStatus.CANCEL_REQUESTED) {
            return OrderItemStatus.CANCELED;
        }
        return requestedStatus;
    }

    private void checkPermission(OrderItem orderItem, OrderItemStatus newStatus) {
        if (UserUtil.isAdmin()) {
            return;
        }

        UUID userId = UserUtil.getUserId();

        if (orderItem.getOrder().getUserUuid().equals(userId)) {
            if (!OrderItemStatus.isUserActionAllowed(newStatus)) {
                throw new OrderItemActionNotAllowedException();
            }
            return;
        }

        if (orderItem.getSellerUuid().equals(userId)) {
            if (!OrderItemStatus.isSellerActionAllowed(newStatus)) {
                throw new OrderItemActionNotAllowedException();
            }
            return;
        }

        throw new OrderAccessDeniedException();
    }

    private void publishEvent(OrderItem orderItem, OrderItemStatus newStatus) {
        switch (newStatus) {
            case CANCELED -> {
                eventPublisher.publish(new OrderItemCanceledEvent(orderItem.getUuid()));
                // 배송 전 취소 시 상품 판매 상태를 즉시 복구한다.
                eventPublisher.publish(new OrderProductSaleReleasedEvent(
                        orderItem.getOrder().getUuid(),
                        List.of(orderItem.getProductUuid())
                ));
            }
            case CONFIRMED -> eventPublisher.publish(new OrderItemConfirmedEvent(orderItem.getUuid()));
            case REFUND_REQUESTED -> eventPublisher.publish(new OrderItemRefundRequestedEvent(orderItem.getUuid()));
        }
    }

    private void syncOrderStatusIfAllItemsCanceled(OrderItem orderItem, OrderItemStatus targetStatus) {
        if (targetStatus != OrderItemStatus.CANCELED) {
            return;
        }

        Order order = orderItem.getOrder();
        boolean allItemsCanceled = order.getOrderItems().stream()
                .allMatch(item -> item.getStatus() == OrderItemStatus.CANCELED);

        if (!allItemsCanceled) {
            return;
        }

        if (order.getStatus() == OrderStatus.CANCELED) {
            return;
        }

        order.updateOrderStatus(OrderStatus.CANCELED);
    }
}
