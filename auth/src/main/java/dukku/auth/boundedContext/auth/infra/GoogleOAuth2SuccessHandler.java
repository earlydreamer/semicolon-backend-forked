package dukku.auth.boundedContext.auth.infra;

import dukku.auth.boundedContext.auth.dto.TokenResponse;
import dukku.auth.boundedContext.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * 구글 OAuth2 로그인 성공 시 토큰을 프런트 콜백 URL로 전달하는 핸들러입니다.
 */
public class GoogleOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${custom.auth.oauth2.success-redirect-url:https://${PUBLIC_WEB_HOST:dukku.earlydreamer.dev}/oauth/google/callback}")
    private String successRedirectUrl;

    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null || email.isBlank()) {
            log.error("구글 OAuth2 로그인에 실패했습니다. 이메일 클레임이 없습니다.");
            response.sendRedirect(buildFailureRedirect("missing_email"));
            return;
        }

        TokenResponse tokenResponse;
        try {
            tokenResponse = authService.loginWithGoogle(email, name);
        } catch (Exception e) {
            log.error("구글 OAuth2 로그인 처리에 실패했습니다. email={}", email, e);
            response.sendRedirect(buildFailureRedirect("social_login_failed"));
            return;
        }

        // 토큰은 쿼리스트링이 아닌 fragment로 전달한다.
        // fragment는 서버 요청·프록시 로그·Referer에 실리지 않고, 프런트가 읽은 직후 주소창에서 지운다.
        String redirectUrl = UriComponentsBuilder.fromUriString(successRedirectUrl)
                .fragment(buildTokenFragment(tokenResponse))
                .build()
                .toUriString();

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String buildTokenFragment(TokenResponse tokenResponse) {
        String fragment = "accessToken=" + URLEncoder.encode(tokenResponse.accessToken(), StandardCharsets.UTF_8);
        if (tokenResponse.refreshToken() != null) {
            fragment += "&refreshToken=" + URLEncoder.encode(tokenResponse.refreshToken(), StandardCharsets.UTF_8);
        }
        return fragment;
    }

    private String buildFailureRedirect(String reason) {
        return UriComponentsBuilder.fromUriString(successRedirectUrl)
                .queryParam("error", reason)
                .build(true)
                .toUriString();
    }
}
