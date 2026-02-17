package dukku.user.boundedContext.user.app.sanction;

import dukku.user.boundedContext.user.entity.User;
import dukku.user.boundedContext.user.entity.UserSanction;
import dukku.user.boundedContext.user.type.UserSanctionAuditAction;
import dukku.user.boundedContext.user.type.UserSanctionReasonCode;
import dukku.user.boundedContext.user.type.UserSanctionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateUserSanctionUseCase {

    private final UserSanctionSupport userSanctionSupport;

    public UserSanction execute(
            UUID userUuid,
            UserSanctionType sanctionType,
            UserSanctionReasonCode reasonCode,
            String memo,
            String evidenceUrl,
            LocalDateTime startAt,
            LocalDateTime endAt,
            UUID actor
    ) {
        LocalDateTime effectiveStartAt = startAt == null ? LocalDateTime.now() : startAt;
        userSanctionSupport.validatePeriod(sanctionType, effectiveStartAt, endAt);

        User user = userSanctionSupport.getUserByUuid(userUuid);
        UserSanction sanction = UserSanction.apply(
                user,
                sanctionType,
                reasonCode,
                memo,
                evidenceUrl,
                effectiveStartAt,
                endAt,
                actor
        );

        UserSanction saved = userSanctionSupport.save(sanction);
        userSanctionSupport.refreshUserStatus(user, LocalDateTime.now());
        userSanctionSupport.writeAudit(saved, UserSanctionAuditAction.APPLIED, actor, memo);
        return saved;
    }
}
