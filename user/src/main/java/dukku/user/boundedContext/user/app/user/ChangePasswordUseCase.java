package dukku.user.boundedContext.user.app.user;

import dukku.common.global.UserUtil;
import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.user.dto.PasswordUpdateRequest;
import dukku.common.shared.user.event.UserModifiedEvent;
import dukku.common.shared.user.exception.UserNotFoundException;
import dukku.user.boundedContext.user.entity.User;
import dukku.user.boundedContext.user.exception.UserPasswordMismatchException;
import dukku.user.boundedContext.user.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;

    @Transactional
    public void execute(PasswordUpdateRequest request) {
        UUID currentUserId = UserUtil.getUserId();

        User user = userRepository.findByUuidAndDeletedAtIsNull(currentUserId)
                .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UserPasswordMismatchException();
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));

        eventPublisher.publish(new UserModifiedEvent(User.toUserDto(user)));
    }
}
