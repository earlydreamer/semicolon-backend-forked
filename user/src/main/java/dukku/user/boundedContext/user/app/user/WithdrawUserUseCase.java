package dukku.user.boundedContext.user.app.user;

import dukku.user.boundedContext.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WithdrawUserUseCase {

    private final UserSupport userSupport;

    @Value("${custom.user.withdrawal.mask-domain:withdrawn.invalid}")
    private String withdrawalMaskDomain;

    public void withdraw(UUID userUuid) {
        User user = userSupport.getActiveUserByUuid(userUuid);
        String maskedEmail = buildMaskedEmail(user.getUuid());
        String maskedNickname = buildMaskedNickname(user.getUuid());
        String encodedPassword = userSupport.encode("WITHDRAWN-" + user.getUuid());
        user.withdraw(maskedEmail, maskedNickname, encodedPassword);
    }

    private String buildMaskedEmail(UUID userUuid) {
        return "withdrawn+" + userUuid + "@" + withdrawalMaskDomain;
    }

    private String buildMaskedNickname(UUID userUuid) {
        String shortId = userUuid.toString().replace("-", "").substring(0, 8);
        return "withdrawn_" + shortId;
    }
}
