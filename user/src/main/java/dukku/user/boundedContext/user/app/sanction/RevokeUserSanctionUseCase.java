package dukku.user.boundedContext.user.app.sanction;

import dukku.user.boundedContext.user.entity.User;
import dukku.user.boundedContext.user.entity.UserSanction;
import dukku.user.boundedContext.user.type.UserSanctionAuditAction;
import dukku.user.boundedContext.user.type.UserSanctionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RevokeUserSanctionUseCase {

    private final UserSanctionSupport userSanctionSupport;

    public UserSanction execute(UUID userUuid, Integer sanctionId, UUID actor, String memo) {
        User user = userSanctionSupport.getUserByUuid(userUuid);
        UserSanction sanction = userSanctionSupport.getSanction(sanctionId, user);

        if (sanction.getStatus() == UserSanctionStatus.ACTIVE) {
            sanction.revoke(actor, LocalDateTime.now());
        }

        userSanctionSupport.refreshUserStatus(user, LocalDateTime.now());
        userSanctionSupport.writeAudit(sanction, UserSanctionAuditAction.REVOKED, actor, memo);
        return sanction;
    }
}
