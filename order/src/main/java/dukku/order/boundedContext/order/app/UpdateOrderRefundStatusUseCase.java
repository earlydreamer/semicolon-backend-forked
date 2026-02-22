package dukku.order.boundedContext.order.app;

import dukku.common.shared.order.exception.OrderRefundAmountOutOfRangeException;
import dukku.common.shared.order.exception.OrderRefundRequestInvalidException;
import dukku.common.shared.order.type.OrderItemStatus;
import dukku.common.shared.order.type.OrderStatus;
import dukku.common.shared.order.type.ReturnStatus;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.out.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 환불 이벤트 기준 주문 및 반품 상태 동기화 유스케이스
 */
@Component
@RequiredArgsConstructor
public class UpdateOrderRefundStatusUseCase {

    private final OrderSupport orderSupport;
    private final ReturnRequestRepository returnRequestRepository;

    /**
     * 환불 완료 이벤트 반영 처리
     */
    @Transactional
    public void updateRefund(UUID refundUuid, UUID orderUuid, Long refundAmount, List<UUID> refundedItemUuids) {
        if (refundUuid == null || orderUuid == null || refundAmount == null || refundAmount <= 0) {
            throw new OrderRefundRequestInvalidException();
        }

        if (!orderSupport.tryMarkRefundCompleted(refundUuid, orderUuid, refundAmount)) {
            return;
        }

        Order order = orderSupport.findOrderByUuidWithItems(orderUuid);

        if (refundAmount > Integer.MAX_VALUE) {
            throw new OrderRefundAmountOutOfRangeException();
        }

        order.updateRefundedAmount(refundAmount.intValue());

        if (refundedItemUuids != null && !refundedItemUuids.isEmpty()) {
            order.getOrderItems().forEach(item -> {
                if (refundedItemUuids.contains(item.getUuid())) {
                    item.updateOrderStatus(OrderItemStatus.REFUND_COMPLETED);
                }
            });

            returnRequestRepository.findByOrderUuid(orderUuid).forEach(req -> {
                if (req.getStatus() == ReturnStatus.RETURN_APPROVED) {
                    boolean allRefunded = req.getReturnItems().stream()
                            .allMatch(ri -> ri.getOrderItem().getStatus() == OrderItemStatus.REFUND_COMPLETED);
                    if (allRefunded) {
                        req.complete();
                    }
                }
            });
        }

        if (order.getStatus() == OrderStatus.CANCELED) {
            return;
        }

        if (order.getRefundedAmount() >= order.getTotalAmount()) {
            order.updateOrderStatus(OrderStatus.CANCELED);
            return;
        }

        order.updateOrderStatus(OrderStatus.PARTIAL_REFUNDED);
    }

    /**
     * 환불 실패 이벤트 보상 처리
     */
    @Transactional
    public void failRefund(UUID orderUuid) {
        if (orderUuid == null) {
            return;
        }

        returnRequestRepository.findByOrderUuid(orderUuid).forEach(req -> {
            if (req.getStatus() == ReturnStatus.RETURN_APPROVED) {
                req.rejectAfterShipment("PG 환불 실패");

                req.getReturnItems().forEach(ri -> {
                    OrderItemStatus currentStatus = ri.getOrderItem().getStatus();
                    if (currentStatus == OrderItemStatus.REFUND_REQUESTED
                            || currentStatus == OrderItemStatus.REFUND_IN_PROGRESS) {
                        ri.getOrderItem().updateOrderStatus(OrderItemStatus.DELIVERED);
                    }
                });
            }
        });
    }
}
