package dukku.user.boundedContext.user.app.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CompensateUserRegistrationUseCase {

    private final UserSupport userSupport;

    public void hardDelete(UUID userUuid) {
        userSupport.hardDeleteByUuid(userUuid);
    }
}
