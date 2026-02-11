package dukku.order.boundedContext.order.in;

import dukku.order.boundedContext.order.app.FindOrderByUuidUseCase;
import dukku.order.boundedContext.order.entity.Order;
import dukku.common.shared.user.dto.UserProfileResponse;
import dukku.common.shared.user.out.UserApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderUserController {

    private final FindOrderByUuidUseCase findOrderByUuidUseCase;
    private final UserApiClient userApiClient;

    @GetMapping("/{orderUuid}/user")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable UUID orderUuid) {
        Order order = findOrderByUuidUseCase.execute(orderUuid);
        UserProfileResponse response = userApiClient.getUserProfile(order.getUserUuid());
        return ResponseEntity.ok(response);
    }
}
