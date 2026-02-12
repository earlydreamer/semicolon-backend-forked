package dukku.user.boundedContext.user.app.user;

import dukku.common.shared.user.dto.UserRegisterRequest;
import dukku.user.boundedContext.user.app.email.EmailVerificationService;
import dukku.user.boundedContext.user.entity.User;
import dukku.common.shared.user.type.Role;
import dukku.common.shared.user.exception.UserConflictException;
import dukku.common.shared.user.event.UserJoinedEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserSupport support;
    private final ApplicationEventPublisher springEventPublisher;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public User execute(UserRegisterRequest req, Role role) {
        emailVerificationService.assertVerifiedForRegister(req.getEmail());
        User userCandidate = support.findByEmail(req.getEmail())
                .map(existing -> restoreOrFail(existing))
                .orElseGet(() -> createNew(req, role));

        User saved = support.save(userCandidate);
        // 회원가입 완료 스프링 이벤트 발행
        springEventPublisher.publishEvent(new UserJoinedEvent(User.toUserDto(saved)));
        return saved;
    }

    private User restoreOrFail(User user) {
        throw new UserConflictException();
    }

    private User createNew(UserRegisterRequest req, Role role) {
        String encoded = support.encode(req.getPassword());
        return User.createUser(req, role, encoded);
    }
}

