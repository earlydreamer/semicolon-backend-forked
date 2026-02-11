package dukku.order.boundedContext.order.in;

import dukku.common.shared.order.dto.ConfirmedOrderItemResponse;
import dukku.order.boundedContext.order.app.OrderFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

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
}
