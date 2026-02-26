package dukku.common.global.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 현재 HTTP 요청의 Authorization 헤더를 조회한다.
 *
 * <p>
 * 사용자 요청 컨텍스트가 없는 배치/비동기 실행 환경에서는 null을 반환한다.
 */
public final class RequestAuthorizationHeaderResolver {

    private static final String ENV_INTERNAL_SERVICE_TOKEN = "INTERNAL_SERVICE_TOKEN";

    private RequestAuthorizationHeaderResolver() {
    }

    public static String resolve() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return resolveFromEnv();
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            return resolveFromEnv();
        }

        return normalizeBearer(authorization);
    }

    private static String resolveFromEnv() {
        String token = System.getenv(ENV_INTERNAL_SERVICE_TOKEN);
        if (token == null || token.isBlank()) {
            return null;
        }
        return normalizeBearer(token);
    }

    private static String normalizeBearer(String token) {
        String normalized = token.trim();
        if (normalized.startsWith("Bearer ")) {
            return normalized;
        }
        return "Bearer " + normalized;
    }
}
