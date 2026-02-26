package dukku.order.boundedContext.order.app;

import dukku.common.global.UserUtil;
import dukku.common.shared.order.dto.OrderListResponse;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.entity.ReturnRequest;
import dukku.order.boundedContext.order.out.OrderRepository;
import dukku.order.boundedContext.order.out.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindMyOrderListUseCase {
    private final OrderRepository orderRepository;
    private final ReturnRequestRepository returnRequestRepository;

    public Page<OrderListResponse> execute(Pageable pageable) {
        UUID currentUserId = UserUtil.getUserId();

        Page<Order> orders = orderRepository.findAllMyOrders(currentUserId, pageable);
        List<UUID> orderUuids = orders.getContent().stream()
                .map(Order::getUuid)
                .toList();

        Map<UUID, ReturnRequest> latestReturnByOrderUuid = new HashMap<>();
        if (!orderUuids.isEmpty()) {
            returnRequestRepository
                    .findAllByUserUuidAndOrderUuidInOrderByCreatedAtDesc(currentUserId, orderUuids)
                    .forEach(returnRequest -> latestReturnByOrderUuid.putIfAbsent(
                            returnRequest.getOrder().getUuid(),
                            returnRequest
                    ));
        }

        List<OrderListResponse> content = orders.getContent().stream()
                .map(Order::fromOrderListResponse)
                .map(response -> {
                    ReturnRequest latestReturn = latestReturnByOrderUuid.get(response.getOrderUuid());
                    return OrderListResponse.builder()
                        .orderUuid(response.getOrderUuid())
                        .returnRequestUuid(latestReturn == null ? null : latestReturn.getUuid())
                        .returnStatus(latestReturn == null ? null : latestReturn.getStatus())
                        .returnCarrierName(latestReturn == null ? null : latestReturn.getCarrierName())
                        .returnTrackingNumber(latestReturn == null ? null : latestReturn.getTrackingNumber())
                        .orderDate(response.getOrderDate())
                        .status(response.getStatus())
                        .totalAmount(response.getTotalAmount())
                        .items(response.getItems())
                        .build();
                })
                .toList();

        return new PageImpl<>(content, pageable, orders.getTotalElements());
    }
}
