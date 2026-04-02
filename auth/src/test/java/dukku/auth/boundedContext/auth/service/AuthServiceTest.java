package dukku.auth.boundedContext.auth.service;

import dukku.auth.boundedContext.auth.dto.LoginRequest;
import dukku.auth.boundedContext.auth.dto.TokenResponse;
import dukku.auth.boundedContext.auth.exception.InvalidRefreshTokenException;
import dukku.auth.boundedContext.auth.infra.UserClient;
import dukku.auth.boundedContext.auth.jwt.AuthTokenIssuer;
import dukku.common.global.exception.UnauthorizedException;
import dukku.common.shared.user.dto.UserVerificationResponse;
import dukku.common.shared.user.type.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final UUID USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ADMIN_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String USER_EMAIL = "user@example.com";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String PASSWORD = "Password1!";
    private static final String OLD_REFRESH_TOKEN = "old-refresh-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String ROTATED_ACCESS_TOKEN = "rotated-access-token";
    private static final String ROLE_NAME = Role.USER.name();
    private static final String ADMIN_ROLE_NAME = Role.ADMIN.name();

    @Mock
    private UserClient userClient;

    @Mock
    private AuthTokenIssuer authTokenIssuer;

    @Mock
    private RefreshTokenStoreService refreshTokenStoreService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("일반 로그인은 USER 역할만 허용한다")
    void loginRejectsWrongRole() {
        LoginRequest request = loginRequest(USER_EMAIL, PASSWORD);
        when(userClient.verifyUser(USER_EMAIL, PASSWORD)).thenReturn(userVerification(ADMIN_UUID, Role.ADMIN));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("잘못된 인증 정보입니다.");

        verify(authTokenIssuer, never()).createAccessToken(any(), any());
        verify(refreshTokenStoreService, never()).save(any(), any(), any());
    }

    @Test
    @DisplayName("관리자 로그인은 ADMIN 역할만 허용한다")
    void loginAdminRejectsWrongRole() {
        LoginRequest request = loginRequest(ADMIN_EMAIL, PASSWORD);
        when(userClient.verifyUser(ADMIN_EMAIL, PASSWORD)).thenReturn(userVerification(USER_UUID, Role.USER));

        assertThatThrownBy(() -> authService.loginAdmin(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("잘못된 인증 정보입니다.");

        verify(authTokenIssuer, never()).createAccessToken(any(), any());
        verify(refreshTokenStoreService, never()).save(any(), any(), any());
    }

    @Test
    @DisplayName("refresh는 재발급 가능 구간에서 refresh 토큰을 회전한다")
    void refreshRotatesTokenOutsideReissueBlockWindow() {
        Claims claims = mock(Claims.class);
        long absoluteExpiryMillis = System.currentTimeMillis() + Duration.ofDays(10).toMillis();

        when(claims.getSubject()).thenReturn(USER_UUID.toString());
        when(claims.get("ROLE", String.class)).thenReturn(ROLE_NAME);
        when(authTokenIssuer.parseRefreshClaims(OLD_REFRESH_TOKEN)).thenReturn(claims);
        when(refreshTokenStoreService.get(USER_UUID)).thenReturn(OLD_REFRESH_TOKEN);
        when(authTokenIssuer.getRefreshTokenAbsoluteExpiryMillis(OLD_REFRESH_TOKEN)).thenReturn(absoluteExpiryMillis);
        when(authTokenIssuer.createAccessToken(USER_UUID, ROLE_NAME)).thenReturn(ROTATED_ACCESS_TOKEN);
        when(authTokenIssuer.createRefreshToken(USER_UUID, ROLE_NAME, absoluteExpiryMillis)).thenReturn(NEW_REFRESH_TOKEN);
        when(authTokenIssuer.getRefreshTokenTtlMillis(NEW_REFRESH_TOKEN)).thenReturn(Duration.ofDays(7).toMillis());

        TokenResponse response = authService.refresh(OLD_REFRESH_TOKEN);

        assertThat(response.accessToken()).isEqualTo(ROTATED_ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
        verify(refreshTokenStoreService).save(eq(USER_UUID), eq(NEW_REFRESH_TOKEN), eq(Duration.ofDays(7)));
    }

    @Test
    @DisplayName("refresh는 절대 만료가 가까우면 refresh 토큰을 그대로 유지한다")
    void refreshKeepsTokenWithinReissueBlockWindow() {
        Claims claims = mock(Claims.class);
        long absoluteExpiryMillis = System.currentTimeMillis() + Duration.ofDays(2).toMillis();

        when(claims.getSubject()).thenReturn(USER_UUID.toString());
        when(claims.get("ROLE", String.class)).thenReturn(ROLE_NAME);
        when(authTokenIssuer.parseRefreshClaims(OLD_REFRESH_TOKEN)).thenReturn(claims);
        when(refreshTokenStoreService.get(USER_UUID)).thenReturn(OLD_REFRESH_TOKEN);
        when(authTokenIssuer.getRefreshTokenAbsoluteExpiryMillis(OLD_REFRESH_TOKEN)).thenReturn(absoluteExpiryMillis);
        when(authTokenIssuer.createAccessToken(USER_UUID, ROLE_NAME)).thenReturn(ACCESS_TOKEN);

        TokenResponse response = authService.refresh(OLD_REFRESH_TOKEN);

        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(OLD_REFRESH_TOKEN);
        verify(authTokenIssuer, never()).createRefreshToken(eq(USER_UUID), eq(ROLE_NAME), anyLong());
        verify(refreshTokenStoreService, never()).save(any(), any(), any());
    }

    @Test
    @DisplayName("refresh 저장값이 다르면 해당 사용자의 refresh 세션을 삭제한다")
    void refreshMismatchDeletesStoredToken() {
        Claims claims = mock(Claims.class);

        when(claims.getSubject()).thenReturn(USER_UUID.toString());
        when(claims.get("ROLE", String.class)).thenReturn(ROLE_NAME);
        when(authTokenIssuer.parseRefreshClaims(OLD_REFRESH_TOKEN)).thenReturn(claims);
        when(refreshTokenStoreService.get(USER_UUID)).thenReturn("different-token");

        assertThatThrownBy(() -> authService.refresh(OLD_REFRESH_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenStoreService).delete(USER_UUID);
        verify(authTokenIssuer, never()).createAccessToken(any(), any());
    }

    @Test
    @DisplayName("사용자 단위 세션 폐기는 저장된 refresh 토큰을 삭제한다")
    void revokeAllSessionsDeletesStoredToken() {
        authService.revokeAllSessions(USER_UUID);

        verify(refreshTokenStoreService).delete(USER_UUID);
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private UserVerificationResponse userVerification(UUID userUuid, Role role) {
        return UserVerificationResponse.builder()
                .userUuid(userUuid)
                .role(role)
                .nickname("nickname")
                .build();
    }
}
