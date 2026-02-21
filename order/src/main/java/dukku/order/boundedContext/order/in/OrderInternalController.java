package dukku.order.boundedContext.order.in;

import dukku.common.shared.order.dto.ConfirmedOrderItemResponse;
import dukku.common.shared.order.dto.OrderListResponse;
import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.app.OrderFacade;
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

    @GetMapping("/items/confirmed")
    public List<ConfirmedOrderItemResponse> findConfirmedItems(
            @RequestParam LocalDateTime startDateTime,
            @RequestParam LocalDateTime endDateTime
    ) {
        return orderFacade.findConfirmedItems(startDateTime, endDateTime);
    }

    @GetMapping("/{userUuid}")
    public List<OrderListResponse> findOrders(
            @PathVariable UUID userUuid,
            @RequestParam OrderStatus status,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return orderFacade.findOrderListByOrderStatus(userUuid, status, limit);
    }
}
