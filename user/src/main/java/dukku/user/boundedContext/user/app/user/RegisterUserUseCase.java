package dukku.user.boundedContext.user.app.user;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.user.dto.UserRegisterRequest;
import dukku.common.shared.user.event.UserJoinedEvent;
import dukku.common.shared.user.exception.UserConflictException;
import dukku.common.shared.user.type.Role;
import dukku.user.boundedContext.user.app.email.EmailVerificationService;
import dukku.user.boundedContext.user.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserSupport support;
    private final EventPublisher eventPublisher;
    private final EmailVerificationService emailVerificationService;
    private final SignupRequestGuard signupRequestGuard;
    private final SignupIdempotencyService signupIdempotencyService;

    @Transactional
    public User execute(UserRegisterRequest req, Role role, String idempotencyKey) {
        SignupIdempotencyService.SignupIdempotencyContext idempotencyContext =
                signupIdempotencyService.begin(idempotencyKey, req);
        if (idempotencyContext.isAlreadyCompleted()) {
            return support.findByEmail(req.getEmail())
                    .orElseThrow(UserConflictException::new);
        }

        String lockToken = signupRequestGuard.acquire(req.getEmail());
        try {
            emailVerificationService.assertVerifiedForRegister(req.getEmail());
            User userCandidate = support.findByEmail(req.getEmail())
                    .map(this::restoreOrFail)
                    .orElseGet(() -> createNew(req, role));

            User saved = saveOrThrowConflict(userCandidate);
            eventPublisher.publish(new UserJoinedEvent(User.toUserDto(saved)));
            signupIdempotencyService.markCompleted(idempotencyContext);
            return saved;
        } catch (RuntimeException e) {
            signupIdempotencyService.rollback(idempotencyContext);
            throw e;
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
