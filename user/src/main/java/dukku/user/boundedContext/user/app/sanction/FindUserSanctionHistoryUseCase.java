package dukku.user.boundedContext.user.app.sanction;

import dukku.user.boundedContext.user.entity.User;
import dukku.user.boundedContext.user.entity.UserSanction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FindUserSanctionHistoryUseCase {

    private final UserSanctionSupport userSanctionSupport;

    @Transactional(readOnly = true)
    public List<UserSanction> execute(UUID userUuid) {
        User user = userSanctionSupport.getUserByUuid(userUuid);
        return userSanctionSupport.getSanctionHistory(user);
    }
}
