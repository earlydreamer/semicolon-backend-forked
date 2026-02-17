package dukku.user.boundedContext.user.in;

import dukku.common.global.UserUtil;
import dukku.user.boundedContext.user.app.user.UserFacade;
import dukku.user.boundedContext.user.app.sanction.UserSanctionFacade;
import dukku.common.shared.user.docs.UserApiDocs;
import dukku.common.shared.user.dto.UserWithdrawalRestoreRequest;
import dukku.user.boundedContext.user.in.dto.AdminUserSanctionCreateRequest;
import dukku.user.boundedContext.user.in.dto.AdminUserSanctionResponse;
import dukku.user.boundedContext.user.in.dto.AdminUserSanctionRevokeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@UserApiDocs.AdminUserTag
public class AdminUserController {

    private final UserFacade userFacade;
    private final UserSanctionFacade userSanctionFacade;

    @PostMapping("/{userUuid}/withdrawal/restore")
    @UserApiDocs.RestoreWithdrawnUser
    public ResponseEntity<Void> restoreWithdrawnUser(
            @PathVariable UUID userUuid,
            @RequestBody @Validated UserWithdrawalRestoreRequest request
    ) {
        userFacade.restoreWithdrawnUser(userUuid, request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userUuid}/sanctions")
    public ResponseEntity<AdminUserSanctionResponse> createUserSanction(
            @PathVariable UUID userUuid,
            @RequestBody @Validated AdminUserSanctionCreateRequest request
    ) {
        // 관리자 행위자 UUID를 저장해 제재 감사 로그와 운영 추적성을 맞춘다.
        UUID actor = UserUtil.getUserId();
        return ResponseEntity.ok(userSanctionFacade.create(userUuid, request, actor));
    }

    @PatchMapping("/{userUuid}/sanctions/{sanctionId}/revoke")
    public ResponseEntity<AdminUserSanctionResponse> revokeUserSanction(
            @PathVariable UUID userUuid,
            @PathVariable Integer sanctionId,
            @RequestBody(required = false) AdminUserSanctionRevokeRequest request
    ) {
        UUID actor = UserUtil.getUserId();
        return ResponseEntity.ok(userSanctionFacade.revoke(userUuid, sanctionId, request, actor));
    }

    @GetMapping("/{userUuid}/sanctions")
    public ResponseEntity<List<AdminUserSanctionResponse>> getUserSanctionHistory(@PathVariable UUID userUuid) {
        return ResponseEntity.ok(userSanctionFacade.getHistory(userUuid));
    }
}
