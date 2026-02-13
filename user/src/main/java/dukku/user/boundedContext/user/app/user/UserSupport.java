package dukku.user.boundedContext.user.app.user;

import dukku.common.shared.user.exception.UserNotFoundException;
import dukku.user.boundedContext.user.entity.User;
import dukku.user.boundedContext.user.exception.InactiveUserException;
import dukku.user.boundedContext.user.out.UserRepository;
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
                .orElseThrow(UserNotFoundException::new);
    }

    public User getActiveUserByUuid(UUID userUuid) {
        return repository.findByUuidAndDeletedAtIsNull(userUuid)
                .orElseThrow(InactiveUserException::new);
    }

    public boolean isActiveEmailInUse(String email, Integer excludeUserId) {
        return repository.existsByEmailAndDeletedAtIsNullAndIdNot(email, excludeUserId);
    }
}
