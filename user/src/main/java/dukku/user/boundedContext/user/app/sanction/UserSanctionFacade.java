package dukku.user.boundedContext.user.app.sanction;

import dukku.user.boundedContext.user.entity.UserSanction;
import dukku.user.boundedContext.user.in.dto.AdminUserSanctionCreateRequest;
import dukku.user.boundedContext.user.in.dto.AdminUserSanctionResponse;
import dukku.user.boundedContext.user.in.dto.AdminUserSanctionRevokeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserSanctionFacade {

    private final CreateUserSanctionUseCase createUserSanctionUseCase;
    private final RevokeUserSanctionUseCase revokeUserSanctionUseCase;
    private final FindUserSanctionHistoryUseCase findUserSanctionHistoryUseCase;

    public AdminUserSanctionResponse create(UUID userUuid, AdminUserSanctionCreateRequest request, UUID actor) {
        UserSanction sanction = createUserSanctionUseCase.execute(
                userUuid,
                request.getSanctionType(),
                request.getReasonCode(),
                request.getMemo(),
                request.getEvidenceUrl(),
                request.getStartAt(),
                request.getEndAt(),
                actor
        );
        return AdminUserSanctionResponse.from(sanction);
    }

    public AdminUserSanctionResponse revoke(UUID userUuid, Integer sanctionId, AdminUserSanctionRevokeRequest request, UUID actor) {
        UserSanction sanction = revokeUserSanctionUseCase.execute(
                userUuid,
                sanctionId,
                actor,
                request == null ? null : request.getMemo()
        );
        return AdminUserSanctionResponse.from(sanction);
    }

    @Transactional(readOnly = true)
    public List<AdminUserSanctionResponse> getHistory(UUID userUuid) {
        return findUserSanctionHistoryUseCase.execute(userUuid).stream()
                .map(AdminUserSanctionResponse::from)
                .toList();
    }
}
