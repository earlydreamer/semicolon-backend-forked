package dukku.common.global.auth.jwt;

import dukku.common.global.auth.detail.CustomUserDetails;
import dukku.common.global.logging.mdc.MdcLoggingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String bearer = request.getHeader("Authorization");
        String token = (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;

        if (token != null && jwtTokenUtil.validateToken(token)) {
            UUID userUuid = jwtTokenUtil.getUserUuid(token);
            String role = jwtTokenUtil.getRole(token);

            CustomUserDetails userDetails = new CustomUserDetails(userUuid, role);

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(auth);

            // MDC에 userId 추가 (로그에 사용자 정보 포함)
            // MdcLoggingFilter가 먼저 실행되어 MDC가 초기화된 상태이므로 안전
            MDC.put(MdcLoggingFilter.USER_ID, userUuid.toString());
            log.debug("User authenticated: {}", userUuid);
        }

        filterChain.doFilter(request, response);
    }
}