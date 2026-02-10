package dukku.common.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * MDC 로깅 필터
 * - 모든 요청에 traceId, spanId 주입
 * - 요청 정보(URI, Method, IP) 주입
 * - 분산 추적을 위한 X-Trace-Id 헤더 전파
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String USER_ID = "userId";
    public static final String REQUEST_URI = "requestUri";
    public static final String REQUEST_METHOD = "requestMethod";
    public static final String CLIENT_IP = "clientIp";

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. TraceId 설정 (헤더에서 가져오거나 새로 생성)
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.isBlank()) {
                traceId = generateTraceId();
            }
            MDC.put(TRACE_ID, traceId);

            // 2. SpanId 설정 (항상 새로 생성)
            String spanId = generateSpanId();
            MDC.put(SPAN_ID, spanId);

            // 3. 요청 정보 설정
            MDC.put(REQUEST_URI, request.getRequestURI());
            MDC.put(REQUEST_METHOD, request.getMethod());
            MDC.put(CLIENT_IP, getClientIp(request));

            // 4. 응답 헤더에 TraceId 추가 (프론트엔드에서 확인 가능)
            response.setHeader(TRACE_ID_HEADER, traceId);
            response.setHeader(SPAN_ID_HEADER, spanId);

            // 5. 요청 시작 로그
            log.debug("Request started: {} {} [traceId={}]",
                    request.getMethod(), request.getRequestURI(), traceId);

            filterChain.doFilter(request, response);

        } finally {
            // 6. MDC 정리 (메모리 누수 방지)
            MDC.clear();
        }
    }

    private String generateTraceId() {
        // 32 hex (W3C Trace Context / OpenTelemetry 표준 호환)
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String generateSpanId() {
        // 16 hex (W3C Span ID 표준 호환)
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String getClientIp(HttpServletRequest request) {
        // 1. X-Forwarded-For 확인 (가장 일반적, 여러 IP 가능 → 첫 번째만 추출)
        String ip = extractFirstIp(request.getHeader("X-Forwarded-For"));
        if (isValidIp(ip)) {
            return ip;
        }

        // 2. Proxy-Client-IP 확인 (Apache, 단일 IP만 존재)
        ip = request.getHeader("Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip;
        }

        // 3. WL-Proxy-Client-IP 확인 (WebLogic, 단일 IP만 존재)
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip;
        }

        // 4. HTTP_CLIENT_IP 확인 (단일 IP만 존재)
        ip = request.getHeader("HTTP_CLIENT_IP");
        if (isValidIp(ip)) {
            return ip;
        }

        // 5. HTTP_X_FORWARDED_FOR 확인 (여러 IP 가능 → 첫 번째만 추출)
        ip = extractFirstIp(request.getHeader("HTTP_X_FORWARDED_FOR"));
        if (isValidIp(ip)) {
            return ip;
        }

        // 6. RemoteAddr 사용 (프록시 없을 때)
        return request.getRemoteAddr();
    }

    /**
     * X-Forwarded-For 헤더에서 첫 번째 IP만 추출
     *
     * 예시:
     *   "203.0.113.1, 198.51.100.2" → "203.0.113.1"
     *   "203.0.113.1" → "203.0.113.1"
     *
     * @param header X-Forwarded-For 헤더 값
     * @return 첫 번째 IP 주소 또는 null
     */
    private String extractFirstIp(String header) {
        if (header == null || header.isEmpty()) {
            return null;
        }

        // 쉼표로 분리하여 첫 번째 IP만 추출
        String[] ips = header.split(",");
        String firstIp = ips[0].trim();

        // 빈 문자열 체크
        return firstIp.isEmpty() ? null : firstIp;
    }


    private boolean isValidIp(String ip) {
        return ip != null
            && !ip.isEmpty()
            && !"unknown".equalsIgnoreCase(ip);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Actuator, Swagger 등 제외
        return path.startsWith("/actuator") ||
               path.startsWith("/swagger") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/favicon.ico");
    }
}
