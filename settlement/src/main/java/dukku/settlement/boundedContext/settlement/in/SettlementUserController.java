package dukku.settlement.boundedContext.settlement.in;

import dukku.settlement.boundedContext.settlement.app.GetSettlementUseCase;
import dukku.settlement.boundedContext.settlement.entity.Settlement;
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
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementUserController {

    private final GetSettlementUseCase getSettlementUseCase;
    private final UserApiClient userApiClient;

    @GetMapping("/{settlementUuid}/user")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable UUID settlementUuid) {
        Settlement settlement = getSettlementUseCase.execute(settlementUuid);
        UserProfileResponse response = userApiClient.getUserProfile(settlement.getSellerUuid());
        return ResponseEntity.ok(response);
    }
}
