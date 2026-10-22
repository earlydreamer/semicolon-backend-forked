package dukku.auth.boundedContext.auth.infra;

import dukku.auth.boundedContext.auth.dto.TokenResponse;
import dukku.auth.boundedContext.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleOAuth2SuccessHandlerTest {

    private static final String CALLBACK_URL = "https://dukku.example.com/oauth/google/callback";

    @Mock
    private AuthService authService;

    private GoogleOAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GoogleOAuth2SuccessHandler(authService);
        ReflectionTestUtils.setField(handler, "successRedirectUrl", CALLBACK_URL);
    }

    @Test
    @DisplayName("로그인 성공 시 토큰을 쿼리스트링이 아닌 fragment로 전달한다")
    void onAuthenticationSuccess_deliversTokensInFragment() throws Exception {
        when(authService.loginWithGoogle("user@example.com", "사용자"))
                .thenReturn(new TokenResponse("access.jwt.value", "refresh-token"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, googleAuthentication());

        String location = response.getRedirectedUrl();
        assertThat(location).isEqualTo(CALLBACK_URL + "#accessToken=access.jwt.value&refreshToken=refresh-token");
        assertThat(location).doesNotContain("?");
    }

    @Test
    @DisplayName("refresh token이 없으면 access token만 fragment로 전달한다")
    void onAuthenticationSuccess_withoutRefreshToken() throws Exception {
        when(authService.loginWithGoogle("user@example.com", "사용자"))
                .thenReturn(new TokenResponse("access.jwt.value", null));

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, googleAuthentication());

        assertThat(response.getRedirectedUrl()).isEqualTo(CALLBACK_URL + "#accessToken=access.jwt.value");
    }

    private TestingAuthenticationToken googleAuthentication() {
        DefaultOAuth2User user = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", "google-sub", "email", "user@example.com", "name", "사용자"),
                "sub"
        );
        return new TestingAuthenticationToken(user, null);
    }
}
