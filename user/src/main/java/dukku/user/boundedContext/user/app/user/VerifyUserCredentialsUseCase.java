package dukku.user.boundedContext.user.app.user;

import dukku.common.shared.user.exception.UserNotFoundException;
import dukku.user.boundedContext.user.entity.User;
import dukku.user.boundedContext.user.exception.InvalidUserCredentialsException;
import dukku.user.boundedContext.user.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerifyUserCredentialsUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User execute(String email, String rawPassword) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidUserCredentialsException();
        }

        return user;
    }
}
