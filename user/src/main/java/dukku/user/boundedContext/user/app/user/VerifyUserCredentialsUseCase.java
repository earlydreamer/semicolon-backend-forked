package dukku.user.boundedContext.user.app.user;

import dukku.common.shared.user.exception.UserInactiveException;
import dukku.common.shared.user.exception.UserInvalidCredentialsException;
import dukku.common.shared.user.exception.UserNotFoundException;
import dukku.common.shared.user.type.UserStatus;
import dukku.user.boundedContext.user.entity.User;
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
            throw new UserInvalidCredentialsException();
        }

        // 브랡리스트 상태(정지/영구정지) 계정은 로그인 단계에서 즉시 차단
        if (user.getStatus() == UserStatus.SUSPENDED
                || user.getStatus() == UserStatus.BANNED
                || user.getStatus() == UserStatus.BLOCKED) {
            throw new UserInactiveException();
        }

        return user;
    }
}
