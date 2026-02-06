package dukku.semicolon.boundedContext.user.app.user;

import dukku.semicolon.boundedContext.user.entity.User;
import dukku.semicolon.boundedContext.user.entity.type.Role;
import dukku.semicolon.shared.user.exception.UserNotFoundException;
import dukku.semicolon.boundedContext.user.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FindUserByRoleUseCase {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User execute(Role role) {
        return userRepository.findByRoleAndDeletedAtIsNull(role)
                .orElseThrow(UserNotFoundException::new);
    }
}
