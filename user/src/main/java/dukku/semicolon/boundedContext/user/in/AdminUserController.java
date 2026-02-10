package dukku.semicolon.boundedContext.user.in;

import dukku.semicolon.boundedContext.user.app.user.UserFacade;
import dukku.common.shared.user.docs.UserApiDocs;
import dukku.common.shared.user.dto.UserWithdrawalRestoreRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@UserApiDocs.AdminUserTag
public class AdminUserController {

    private final UserFacade userFacade;

    @PostMapping("/{userUuid}/withdrawal/restore")
    @UserApiDocs.RestoreWithdrawnUser
    public ResponseEntity<Void> restoreWithdrawnUser(
            @PathVariable UUID userUuid,
            @RequestBody @Validated UserWithdrawalRestoreRequest request
    ) {
        userFacade.restoreWithdrawnUser(userUuid, request.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}
