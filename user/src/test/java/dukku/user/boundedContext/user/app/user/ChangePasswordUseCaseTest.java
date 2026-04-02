package dukku.user.boundedContext.user.app.user;

import dukku.common.global.UserUtil;
import dukku.common.global.auth.detail.CustomUserDetails;
import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.auth.out.AuthApiClient;
import dukku.common.shared.user.dto.PasswordUpdateRequest;
import dukku.common.shared.user.exception.UserPasswordMismatchException;
import dukku.user.boundedContext.user.entity.User;
import dukku.user.boundedContext.user.out.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangePasswordUseCaseTest {

    private static final UUID USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String CURRENT_PASSWORD = "Password1!";
    private static final String NEW_PASSWORD = "Newpass1!";
    private static final String ENCODED_NEW_PASSWORD = "encoded-new-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private AuthApiClient authApiClient;

    @InjectMocks
    private ChangePasswordUseCase changePasswordUseCase;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("비밀번호 변경이 성공하면 사용자 비밀번호를 업데이트한 뒤 세션을 폐기한다")
    void revokesSessionsAfterSuccessfulPasswordChange() {
        setAuthenticatedUser(USER_UUID);

        User user = user();
        PasswordUpdateRequest request = passwordUpdateRequest(CURRENT_PASSWORD, NEW_PASSWORD);

        when(userRepository.findByUuidAndDeletedAtIsNull(USER_UUID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(CURRENT_PASSWORD, user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_NEW_PASSWORD);

        changePasswordUseCase.execute(request);

        InOrder inOrder = inOrder(user, eventPublisher, authApiClient);
        inOrder.verify(user).updatePassword(ENCODED_NEW_PASSWORD);
        inOrder.verify(eventPublisher).publish(any());
        inOrder.verify(authApiClient).revokeAllSessions(USER_UUID);
    }

    @Test
    @DisplayName("현재 비밀번호가 일치하지 않으면 세션을 폐기하지 않는다")
    void doesNotRevokeSessionsWhenCurrentPasswordMismatch() {
        setAuthenticatedUser(USER_UUID);

        User user = org.mockito.Mockito.mock(User.class);
        when(user.getPassword()).thenReturn("current-encoded-password");
        PasswordUpdateRequest request = passwordUpdateRequest(CURRENT_PASSWORD, NEW_PASSWORD);

        when(userRepository.findByUuidAndDeletedAtIsNull(USER_UUID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(CURRENT_PASSWORD, user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> changePasswordUseCase.execute(request))
                .isInstanceOf(UserPasswordMismatchException.class);

        verify(user, never()).updatePassword(any());
        verify(eventPublisher, never()).publish(any());
        verify(authApiClient, never()).revokeAllSessions(any());
    }

    private void setAuthenticatedUser(UUID userUuid) {
        CustomUserDetails userDetails = new CustomUserDetails(userUuid, "USER");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User user() {
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getPassword()).thenReturn("current-encoded-password");
        when(user.getUuid()).thenReturn(USER_UUID);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getNickname()).thenReturn("nickname");
        when(user.getRole()).thenReturn(dukku.common.shared.user.type.Role.USER);
        when(user.getStatus()).thenReturn(dukku.common.shared.user.type.UserStatus.ACTIVE);
        return user;
    }

    private PasswordUpdateRequest passwordUpdateRequest(String currentPassword, String newPassword) {
        PasswordUpdateRequest request = new PasswordUpdateRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        return request;
    }
}
