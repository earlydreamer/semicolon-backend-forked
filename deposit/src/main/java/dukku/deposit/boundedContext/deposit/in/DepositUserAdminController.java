package dukku.semicolon.boundedContext.deposit.in;

import dukku.semicolon.boundedContext.deposit.app.DepositFacade;
import dukku.common.shared.deposit.docs.DepositApiDocs;
import dukku.common.shared.deposit.dto.DepositDto;
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
@RequestMapping("/api/v1/admin/deposits")
@RequiredArgsConstructor
@DepositApiDocs.DepositTag
public class DepositUserAdminController {

    private final DepositFacade depositFacade;
    private final UserApiClient userApiClient;

    @GetMapping("/{depositUuid}/user")
    public ResponseEntity<UserAdminProfileResponse> getUserAdminProfile(@PathVariable UUID depositUuid) {
        DepositDto deposit = depositFacade.findDepositByDepositUuid(depositUuid);
        UserAdminProfileResponse response = userApiClient.getUserAdminProfile(deposit.getUserUuid());
        return ResponseEntity.ok(response);
    }
}
