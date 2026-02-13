package dukku.user.boundedContext.user.app.user;

import dukku.common.shared.user.type.UserStatus;
import dukku.user.boundedContext.user.entity.User;
import dukku.user.boundedContext.user.exception.WithdrawRestoreNotAllowedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RestoreWithdrawnUserUseCase {

    private final UserSupport userSupport;

    @Value("${custom.user.withdrawal.restore-days:30}")
    private long restoreDays;

    public void restore(UUID userUuid, String newPassword) {
        User user = userSupport.getUserByUuid(userUuid);
        assertRestorable(user);

        String backupEmail = user.getWithdrawalEmailBackup();
        if (backupEmail != null && userSupport.isActiveEmailInUse(backupEmail, user.getId())) {
            throw WithdrawRestoreNotAllowedException.emailAlreadyInUse();
        }

        String encodedPassword = userSupport.encode(newPassword);
        user.restoreFromWithdrawal(encodedPassword);
    }

    private void assertRestorable(User user) {
        if (user.getDeletedAt() == null) {
            throw WithdrawRestoreNotAllowedException.userIsNotWithdrawn();
        }
        if (user.getStatus() == UserStatus.WITHDRAWN_FINAL) {
            throw WithdrawRestoreNotAllowedException.restoreWindowExpired();
        }
        LocalDateTime deletedAt = user.getDeletedAt();
        LocalDateTime restoreDeadline = deletedAt.plusDays(restoreDays);
        if (LocalDateTime.now().isAfter(restoreDeadline)) {
            throw WithdrawRestoreNotAllowedException.restoreWindowExpired();
        }
    }
}
