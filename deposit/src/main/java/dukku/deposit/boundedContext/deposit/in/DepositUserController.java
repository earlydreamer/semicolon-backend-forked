package dukku.deposit.boundedContext.deposit.in;

import dukku.deposit.boundedContext.deposit.app.DepositFacade;
import dukku.common.shared.deposit.docs.DepositApiDocs;
import dukku.common.shared.deposit.dto.DepositDto;
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
@RequestMapping("/api/v1/deposits")
@RequiredArgsConstructor
@DepositApiDocs.DepositTag
public class DepositUserController {

    private final DepositFacade depositFacade;
    private final UserApiClient userApiClient;

    @GetMapping("/{depositUuid}/user")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable UUID depositUuid) {
        DepositDto deposit = depositFacade.findDepositByDepositUuid(depositUuid);
        UserProfileResponse response = userApiClient.getUserProfile(deposit.getUserUuid());
        return ResponseEntity.ok(response);
    }
}
