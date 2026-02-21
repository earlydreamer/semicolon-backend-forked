package dukku.order.boundedContext.order.app;

import dukku.common.shared.order.dto.ReturnRequestCreateDto;
import dukku.common.shared.order.dto.ReturnResponse;
import dukku.common.shared.order.exception.OrderAccessDeniedException;
import dukku.common.shared.order.exception.OrderItemNotFoundException;
import dukku.common.shared.order.exception.OrderNotFoundException;
import dukku.common.shared.order.exception.ReturnItemSelectionRequiredException;
import dukku.common.shared.order.type.OrderItemStatus;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.entity.OrderItem;
import dukku.order.boundedContext.order.entity.ReturnItem;
import dukku.order.boundedContext.order.entity.ReturnRequest;
import dukku.order.boundedContext.order.out.OrderRepository;
import dukku.order.boundedContext.order.out.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 구매자 반품 신청 처리 유스케이스
 */
@Service
@RequiredArgsConstructor
public class RequestReturnUseCase {

    private final OrderRepository orderRepository;
    private final ReturnRequestRepository returnRequestRepository;

    /**
     * 주문 소유권 검증 후 반품 신청 생성 처리
     */
    @Transactional
    public ReturnResponse execute(UUID userUuid, UUID orderUuid, ReturnRequestCreateDto dto) {
        Order order = orderRepository.findByUuidWithItems(orderUuid)
                .orElseThrow(OrderNotFoundException::new);

        if (!order.getUserUuid().equals(userUuid)) {
            throw new OrderAccessDeniedException();
        }

        if (dto.getOrderItemUuids() == null || dto.getOrderItemUuids().isEmpty()) {
            throw new ReturnItemSelectionRequiredException();
        }

        ReturnRequest returnRequest = ReturnRequest.create(order, userUuid, dto.getReason());

        for (UUID itemUuid : dto.getOrderItemUuids()) {
            OrderItem orderItem = order.getOrderItems().stream()
                    .filter(item -> item.getUuid().equals(itemUuid))
                    .findFirst()
                    .orElseThrow(OrderItemNotFoundException::new);

            orderItem.updateOrderStatus(OrderItemStatus.REFUND_REQUESTED);
            int refundAmount = orderItem.getProductPrice();

            ReturnItem returnItem = ReturnItem.create(orderItem, refundAmount);
            returnRequest.addReturnItem(returnItem);
        }

        ReturnRequest saved = returnRequestRepository.save(returnRequest);
        return saved.toResponse();
    }
}
