package dukku.user.boundedContext.user.app.sanction;

import dukku.user.boundedContext.user.entity.User;
import dukku.user.boundedContext.user.entity.UserSanction;
import dukku.user.boundedContext.user.type.UserSanctionAuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExpireUserSanctionScheduler {

    private static final UUID SYSTEM_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final UserSanctionSupport userSanctionSupport;

    // 만료된 일시정지를 자동 해제해 회원 상태와 제재 상태를 동기화한다.
    @Scheduled(cron = "${custom.user.sanction.expire-cron:0 */10 * * * *}")
    @Transactional
    public void expireSanctions() {
        LocalDateTime now = LocalDateTime.now();
        List<UserSanction> expiredTargets = userSanctionSupport.findExpiredTargets(now);

        for (UserSanction sanction : expiredTargets) {
            if (!sanction.isEffectiveAt(now)) {
                sanction.expire(now);
                User user = sanction.getUser();
                userSanctionSupport.refreshUserStatus(user, now);
                userSanctionSupport.writeAudit(sanction, UserSanctionAuditAction.EXPIRED, SYSTEM_ACTOR, "제재 기간 만료");
            }
        }
    }
}
