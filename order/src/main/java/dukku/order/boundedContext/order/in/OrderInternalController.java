package dukku.order.boundedContext.order.in;

import dukku.common.shared.order.dto.ConfirmedOrderItemResponse;
import dukku.common.shared.order.dto.OrderListResponse;
import dukku.common.shared.order.dto.OrderResponse;
import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.app.FindOrderByUuidUseCase;
import dukku.order.boundedContext.order.app.OrderFacade;
import dukku.order.boundedContext.order.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/orders")
@RequiredArgsConstructor
public class OrderInternalController {
    private final OrderFacade orderFacade;
    private final FindOrderByUuidUseCase findOrderByUuidUseCase;

    @GetMapping("/items/confirmed")
    public List<ConfirmedOrderItemResponse> findConfirmedItems(
            @RequestParam LocalDateTime startDateTime,
            @RequestParam LocalDateTime endDateTime
    ) {
        return orderFacade.findConfirmedItems(startDateTime, endDateTime);
    }

    @GetMapping("/{userUuid}")
    // 내부 호출용으로 사용자와 상태 기준 주문 목록을 조회한다.
    public List<OrderListResponse> findOrders(
            @PathVariable UUID userUuid,
            @RequestParam OrderStatus status,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return orderFacade.findOrderListByOrderStatus(userUuid, status, limit);
    }

    @GetMapping("/{orderUuid}/detail")
    // 결제 도메인에서 참조할 주문 상세(주문시각 포함)를 반환한다.
    public OrderResponse findOrderByUuid(@PathVariable UUID orderUuid) {
        Order order = findOrderByUuidUseCase.execute(orderUuid);
        return Order.toOrderResponse(order);
    }
}
