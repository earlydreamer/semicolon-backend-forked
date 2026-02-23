package dukku.order.boundedContext.order.in;

import dukku.order.boundedContext.order.app.FindOrderByUuidUseCase;
import dukku.order.boundedContext.order.app.UpdateOrderStatusForAdminUseCase;
import dukku.order.boundedContext.order.entity.Order;
import dukku.common.shared.order.type.OrderStatus;
import dukku.common.shared.user.dto.UserAdminProfileResponse;
import dukku.common.shared.user.out.UserApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class OrderUserAdminController {

    private final FindOrderByUuidUseCase findOrderByUuidUseCase;
    private final UpdateOrderStatusForAdminUseCase updateOrderStatusForAdminUseCase;
    private final UserApiClient userApiClient;

    @GetMapping("/{orderUuid}/user")
    public ResponseEntity<UserAdminProfileResponse> getUserAdminProfile(@PathVariable UUID orderUuid) {
        Order order = findOrderByUuidUseCase.execute(orderUuid);
        UserAdminProfileResponse response = userApiClient.getUserAdminProfile(order.getUserUuid());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{orderUuid}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable UUID orderUuid,
            @RequestParam OrderStatus status
    ) {
        updateOrderStatusForAdminUseCase.execute(orderUuid, status);
        return ResponseEntity.noContent().build();
    }
}
