package dukku.user.boundedContext.user.app.sanction;

import dukku.common.shared.user.type.UserStatus;
import dukku.user.boundedContext.user.app.user.UserSupport;
import dukku.user.boundedContext.user.entity.User;
import dukku.user.boundedContext.user.entity.UserSanction;
import dukku.user.boundedContext.user.entity.UserSanctionAuditLog;
import dukku.user.boundedContext.user.exception.UserSanctionBadRequestException;
import dukku.user.boundedContext.user.exception.UserSanctionNotFoundException;
import dukku.user.boundedContext.user.out.UserSanctionAuditLogRepository;
import dukku.user.boundedContext.user.out.UserSanctionRepository;
import dukku.user.boundedContext.user.type.UserSanctionAuditAction;
import dukku.user.boundedContext.user.type.UserSanctionStatus;
import dukku.user.boundedContext.user.type.UserSanctionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserSanctionSupport {

    private final UserSupport userSupport;
    private final UserSanctionRepository userSanctionRepository;
    private final UserSanctionAuditLogRepository userSanctionAuditLogRepository;

    public User getUserByUuid(UUID userUuid) {
        return userSupport.getUserByUuid(userUuid);
    }

    public UserSanction getSanction(Integer sanctionId, User user) {
        return userSanctionRepository.findByIdAndUser(sanctionId, user)
                .orElseThrow(UserSanctionNotFoundException::new);
    }

    public List<UserSanction> getSanctionHistory(User user) {
        return userSanctionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public void validatePeriod(UserSanctionType sanctionType, LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null) {
            throw UserSanctionBadRequestException.invalidPeriod();
        }

        // 제재 타입별 기간 규칙을 강제해 잘못된 입력을 사전에 차단한다.
        switch (sanctionType) {
            case WARNING -> {
                if (endAt != null) {
                    throw UserSanctionBadRequestException.warningMustNotHaveEndAt();
                }
            }
            case SUSPENSION -> {
                if (endAt == null) {
                    throw UserSanctionBadRequestException.suspensionRequiresEndAt();
                }
                if (!endAt.isAfter(startAt)) {
                    throw UserSanctionBadRequestException.invalidPeriod();
                }
            }
            case PERMANENT_BAN -> {
                if (endAt != null) {
                    throw UserSanctionBadRequestException.permanentBanMustNotHaveEndAt();
                }
            }
        }
    }

    public UserSanction save(UserSanction sanction) {
        return userSanctionRepository.save(sanction);
    }

    public void writeAudit(UserSanction sanction, UserSanctionAuditAction action, UUID actor, String memo) {
        UserSanctionAuditLog auditLog = UserSanctionAuditLog.create(sanction, action, actor, memo);
        userSanctionAuditLogRepository.save(auditLog);
    }

    public void refreshUserStatus(User user, LocalDateTime now) {
        // 탈퇴 계정은 제재 상태 계산 대상에서 제외한다.
        if (user.getStatus() == UserStatus.WITHDRAWN_PENDING
                || user.getStatus() == UserStatus.WITHDRAWN_FINAL
                || user.getStatus() == UserStatus.DELETED) {
            return;
        }

        List<UserSanction> activeSanctions = userSanctionRepository.findByUserAndStatus(user, UserSanctionStatus.ACTIVE);

        // 상태 우선순위: 영구정지 > 일시정지 > 활성
        boolean hasPermanentBan = activeSanctions.stream()
                .anyMatch(sanction -> sanction.getSanctionType() == UserSanctionType.PERMANENT_BAN && sanction.isEffectiveAt(now));
        if (hasPermanentBan) {
            user.updateStatus(UserStatus.BANNED);
            return;
        }

        boolean hasSuspension = activeSanctions.stream()
                .anyMatch(sanction -> sanction.getSanctionType() == UserSanctionType.SUSPENSION && sanction.isEffectiveAt(now));
        if (hasSuspension) {
            user.updateStatus(UserStatus.SUSPENDED);
            return;
        }

        user.updateStatus(UserStatus.ACTIVE);
    }

    public List<UserSanction> findExpiredTargets(LocalDateTime now) {
        return userSanctionRepository.findByStatusAndEndAtBefore(UserSanctionStatus.ACTIVE, now);
    }
}
