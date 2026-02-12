package dukku.settlement.boundedContext.settlement.in;

import dukku.settlement.boundedContext.settlement.app.GetSettlementUseCase;
import dukku.settlement.boundedContext.settlement.entity.Settlement;
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
@RequestMapping("/api/v1/admin/settlements")
@RequiredArgsConstructor
public class SettlementUserAdminController {

    private final GetSettlementUseCase getSettlementUseCase;
    private final UserApiClient userApiClient;

    @GetMapping("/{settlementUuid}/user")
    public ResponseEntity<UserAdminProfileResponse> getUserAdminProfile(@PathVariable UUID settlementUuid) {
        Settlement settlement = getSettlementUseCase.execute(settlementUuid);
        UserAdminProfileResponse response = userApiClient.getUserAdminProfile(settlement.getSellerUuid());
        return ResponseEntity.ok(response);
    }
}
