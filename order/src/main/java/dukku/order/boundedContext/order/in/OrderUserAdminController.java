package dukku.semicolon.boundedContext.order.in;

import dukku.semicolon.boundedContext.order.app.FindOrderByUuidUseCase;
import dukku.semicolon.boundedContext.order.entity.Order;
import dukku.common.shared.user.dto.UserAdminProfileResponse;
import dukku.common.shared.user.out.UserApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class OrderUserAdminController {

    private final FindOrderByUuidUseCase findOrderByUuidUseCase;
    private final UserApiClient userApiClient;

    @GetMapping("/{orderUuid}/user")
    public ResponseEntity<UserAdminProfileResponse> getUserAdminProfile(@PathVariable UUID orderUuid) {
        Order order = findOrderByUuidUseCase.execute(orderUuid);
        UserAdminProfileResponse response = userApiClient.getUserAdminProfile(order.getUserUuid());
        return ResponseEntity.ok(response);
    }
}
