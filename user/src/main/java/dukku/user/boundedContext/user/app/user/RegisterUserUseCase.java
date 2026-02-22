package dukku.user.boundedContext.user.app.user;

import dukku.common.shared.user.dto.UserRegisterRequest;
import dukku.common.shared.user.event.UserJoinedEvent;
import dukku.common.shared.user.exception.UserConflictException;
import dukku.common.shared.user.type.Role;
import dukku.user.boundedContext.user.app.email.EmailVerificationService;
import dukku.user.boundedContext.user.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserSupport support;
    private final ApplicationEventPublisher springEventPublisher;
    private final EmailVerificationService emailVerificationService;
    private final SignupRequestGuard signupRequestGuard;

    @Transactional
    public User execute(UserRegisterRequest req, Role role) {
        String lockToken = signupRequestGuard.acquire(req.getEmail());
        try {
            emailVerificationService.assertVerifiedForRegister(req.getEmail());
            User userCandidate = support.findByEmail(req.getEmail())
                    .map(existing -> restoreOrFail(existing))
                    .orElseGet(() -> createNew(req, role));

            User saved = saveOrThrowConflict(userCandidate);
            springEventPublisher.publishEvent(new UserJoinedEvent(User.toUserDto(saved)));
            return saved;
        } finally {
            signupRequestGuard.release(req.getEmail(), lockToken);
        }
    }

    private User restoreOrFail(User user) {
        throw new UserConflictException();
    }

    private User createNew(UserRegisterRequest req, Role role) {
        String encoded = support.encode(req.getPassword());
        return User.createUser(req, role, encoded);
    }

    private User saveOrThrowConflict(User userCandidate) {
        try {
            return support.save(userCandidate);
        } catch (DataIntegrityViolationException e) {
            throw new UserConflictException();
        }
    }
}
