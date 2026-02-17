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

    // endAt가 지난 제재를 만료 처리해 제재 상태와 회원 상태를 동기화한다.
    @Scheduled(cron = "${custom.user.sanction.expire-cron:0 */10 * * * *}")
    @Transactional
    public void expireSanctions() {
        LocalDateTime now = LocalDateTime.now();
        List<UserSanction> expiredTargets = userSanctionSupport.findExpiredTargets(now);

        for (UserSanction sanction : expiredTargets) {
            // 경계 시각 오차를 방지하기 위해 만료 대상이라도 유효성 검사를 한 번 더 수행한다.
            if (!sanction.isEffectiveAt(now)) {
                sanction.expire(now);
                User user = sanction.getUser();
                userSanctionSupport.refreshUserStatus(user, now);
                userSanctionSupport.writeAudit(
                        sanction,
                        UserSanctionAuditAction.EXPIRED,
                        SYSTEM_ACTOR,
                        "제재 기간 만료"
                );
            }
        }
    }
}
