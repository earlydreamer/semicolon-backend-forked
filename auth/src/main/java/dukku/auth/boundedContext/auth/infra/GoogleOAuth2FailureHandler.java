package dukku.auth.boundedContext.auth.infra;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
/**
 * 구글 OAuth2 로그인 실패를 프런트 콜백 URL로 전달하는 핸들러입니다.
 */
public class GoogleOAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${custom.auth.oauth2.success-redirect-url:https://dukku.earlydreamer.dev/oauth/google/callback}")
    private String successRedirectUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        log.error("구글 OAuth2 인증에 실패했습니다.", exception);

        String redirectUrl = UriComponentsBuilder.fromUriString(successRedirectUrl)
                .queryParam("error", "oauth2_authentication_failed")
                .build(true)
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
