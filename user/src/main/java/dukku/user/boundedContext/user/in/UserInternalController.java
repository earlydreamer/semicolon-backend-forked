package dukku.user.boundedContext.user.in;

import dukku.common.shared.user.dto.UserAdminProfileResponse;
import dukku.common.shared.user.dto.UserProfileResponse;
import dukku.common.shared.user.dto.SocialUserUpsertRequest;
import dukku.common.shared.user.dto.UserUuidResponse;
import dukku.common.shared.user.dto.UserVerificationRequest;
import dukku.common.shared.user.dto.UserVerificationResponse;
import dukku.common.shared.user.type.Role;
import dukku.user.boundedContext.user.app.user.FindUserByEmailUseCase;
import dukku.user.boundedContext.user.app.user.FindUserByRoleUseCase;
import dukku.user.boundedContext.user.app.user.FindUserUseCase;
import dukku.user.boundedContext.user.app.user.UpsertSocialUserUseCase;
import dukku.user.boundedContext.user.app.user.VerifyUserCredentialsUseCase;
import dukku.user.boundedContext.user.entity.User;
import dukku.common.shared.user.exception.UserInvalidLookupRequestException;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
@Hidden
public class UserInternalController {

    private final FindUserByRoleUseCase findUserByRole;
    private final FindUserByEmailUseCase findUserByEmail;
    private final FindUserUseCase findUser;
    private final VerifyUserCredentialsUseCase verifyUserCredentialsUseCase;
    private final UpsertSocialUserUseCase upsertSocialUserUseCase;

    @GetMapping("/uuid")
    public ResponseEntity<UserUuidResponse> getUserUuid(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String email
    ) {
        if ((role == null) == (email == null)) {
            throw UserInvalidLookupRequestException.roleOrEmailOnly();
        }
        if (email != null && email.trim().isEmpty()) {
            throw UserInvalidLookupRequestException.emailBlank();
        }

        User user = role != null
                ? findUserByRole.execute(role)
                : findUserByEmail.execute(email.trim());

        log.info("[Internal API] User UUID lookup. role={}, email={}", role, email);

        UserUuidResponse response = UserUuidResponse.builder()
                .userUuid(user.getUuid())
                .email(user.getEmail())
                .role(user.getRole())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userUuid}/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable UUID userUuid) {
        User user = findUser.execute(userUuid);

        log.info("[Internal API] User profile lookup. userUuid={}", userUuid);

        UserProfileResponse response = UserProfileResponse.builder()
                .userUuid(user.getUuid())
                .nickname(user.getNickname())
                .createdAt(user.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userUuid}/admin")
    public ResponseEntity<UserAdminProfileResponse> getUserAdminProfile(@PathVariable UUID userUuid) {
        User user = findUser.execute(userUuid);

        log.info("[Internal API] User admin profile lookup. userUuid={}", userUuid);

        UserAdminProfileResponse response = UserAdminProfileResponse.builder()
                .userUuid(user.getUuid())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole())
                .status(user.getStatus())
                .statusLabel(user.getStatus().getLabel())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-password")
    public ResponseEntity<UserVerificationResponse> verifyPassword(@RequestBody UserVerificationRequest request) {
        User user = verifyUserCredentialsUseCase.execute(request.getEmail(), request.getPassword());

        log.info("[Internal API] User credentials verified. email={}", request.getEmail());

        return ResponseEntity.ok(UserVerificationResponse.builder()
                .userUuid(user.getUuid())
                .role(user.getRole())
                .nickname(user.getNickname())
                .build());
    }

    @PostMapping("/social")
    public ResponseEntity<UserVerificationResponse> upsertSocialUser(
            @RequestBody @Validated SocialUserUpsertRequest request
    ) {
        UserVerificationResponse response = upsertSocialUserUseCase.execute(request);

        log.info("[내부 API] 소셜 사용자 저장(생성/갱신) 완료. provider={}, email={}", request.getProvider(), request.getEmail());

        return ResponseEntity.ok(response);
    }
}
