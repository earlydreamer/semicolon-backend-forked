package dukku.semicolon.boundedContext.user.app.user;

import dukku.semicolon.boundedContext.user.entity.User;
import dukku.common.shared.user.exception.UserNotFoundException;
import dukku.semicolon.boundedContext.user.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FindUserByEmailUseCase {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User execute(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(UserNotFoundException::new);
    }
}
