package dukku.user.boundedContext.user.app.user;

import dukku.user.boundedContext.user.entity.User;
import dukku.common.shared.user.type.UserStatus;
import dukku.user.boundedContext.user.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WithdrawnUserPurgeScheduler {

    private final UserRepository userRepository;
    private final UserSupport userSupport;

    @Value("${custom.user.withdrawal.purge-days:90}")
    private long purgeDays;

    @Scheduled(cron = "${custom.user.withdrawal.purge-cron:0 0 3 * * *}")
    @Transactional
    public void purgeWithdrawnUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(purgeDays);
        List<User> targets = userRepository.findByStatusInAndDeletedAtBefore(
                List.of(UserStatus.WITHDRAWN_PENDING, UserStatus.DELETED),
                cutoff
        );

        for (User user : targets) {
            String encodedPassword = userSupport.encode("PURGED-" + user.getUuid());
            user.finalizeWithdrawal(encodedPassword);
        }
    }
}
