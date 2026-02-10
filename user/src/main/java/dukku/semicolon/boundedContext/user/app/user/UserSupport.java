package dukku.semicolon.boundedContext.user.app.user;

import dukku.common.global.exception.NotFoundException;
import dukku.common.global.exception.UnauthorizedException;
import dukku.semicolon.boundedContext.user.entity.User;
import dukku.semicolon.boundedContext.user.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserSupport {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public User save(User user) {
        return repository.save(user);
    }

    public String encode(String raw) {
        return passwordEncoder.encode(raw);
    }

    public User getUserByUuid(UUID userUuid) {
        return repository.findByUuid(userUuid)
                .orElseThrow(() -> new NotFoundException("User not found."));
    }

    public User getActiveUserByUuid(UUID userUuid) {
        return repository.findByUuidAndDeletedAtIsNull(userUuid)
                .orElseThrow(() -> new UnauthorizedException("이미 탈퇴한 사용자입니다."));
    }

    public boolean isActiveEmailInUse(String email, Integer excludeUserId) {
        return repository.existsByEmailAndDeletedAtIsNullAndIdNot(email, excludeUserId);
    }
}
