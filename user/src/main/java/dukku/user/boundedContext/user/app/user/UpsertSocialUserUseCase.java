package dukku.user.boundedContext.user.app.user;

import dukku.common.shared.user.dto.SocialUserUpsertRequest;
import dukku.common.shared.user.dto.UserVerificationResponse;
import dukku.common.shared.user.exception.UserInactiveException;
import dukku.common.shared.user.exception.UserInvalidSocialProviderException;
import dukku.common.shared.user.type.Role;
import dukku.common.shared.user.type.SocialProvider;
import dukku.common.shared.user.type.UserStatus;
import dukku.user.boundedContext.user.entity.User;
import dukku.user.boundedContext.user.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpsertSocialUserUseCase {

    private static final String SOCIAL_PASSWORD_PREFIX = "Gg";
    private static final int SOCIAL_PASSWORD_BODY_LENGTH = 14;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserVerificationResponse execute(SocialUserUpsertRequest request) {
        if (request.getProvider() != SocialProvider.GOOGLE) {
            throw UserInvalidSocialProviderException.unsupported(request.getProvider());
        }

        String email = request.getEmail().trim();

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .map(this::validateActive)
                .orElseGet(() -> createUser(email, request.getNickname()));

        return UserVerificationResponse.builder()
                .userUuid(user.getUuid())
                .role(user.getRole())
                .nickname(user.getNickname())
                .build();
    }

    private User validateActive(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED
                || user.getStatus() == UserStatus.BANNED
                || user.getStatus() == UserStatus.BLOCKED) {
            throw new UserInactiveException();
        }
        return user;
    }

    private User createUser(String email, String nickname) {
        String rawPassword = SOCIAL_PASSWORD_PREFIX
                + UUID.randomUUID().toString().replace("-", "").substring(0, SOCIAL_PASSWORD_BODY_LENGTH);
        String resolvedNickname = resolveNickname(email, nickname);

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.USER)
                .nickname(resolvedNickname)
                .build();

        return userRepository.save(user);
    }

    private String resolveNickname(String email, String nickname) {
        if (nickname != null && !nickname.isBlank()) {
            return nickname.trim();
        }

        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            return email.substring(0, atIndex);
        }

        return "user";
    }
}
