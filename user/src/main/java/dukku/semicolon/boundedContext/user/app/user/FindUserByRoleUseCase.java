package dukku.user.boundedContext.user.app.user;

import dukku.user.boundedContext.user.entity.User;
import dukku.common.shared.user.type.Role;
import dukku.common.shared.user.exception.UserNotFoundException;
import dukku.user.boundedContext.user.out.UserRepository;
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
