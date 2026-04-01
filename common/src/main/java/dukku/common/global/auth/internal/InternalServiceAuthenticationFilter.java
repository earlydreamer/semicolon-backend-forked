package dukku.common.global.auth.internal;

import dukku.common.global.auth.InternalServiceTokenResolver;
import dukku.common.global.auth.detail.CustomUserDetails;
import dukku.common.shared.user.type.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

@Component
public class InternalServiceAuthenticationFilter extends OncePerRequestFilter {

    private static final UUID SYSTEM_PRINCIPAL_UUID = new UUID(0L, 0L);
    private static final List<String> INTERNAL_PATH_PREFIXES = List.of(
            "/api/v1/internal/",
            "/api/v1/products/internal/",
            "/api/v1/carts/internal/"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return INTERNAL_PATH_PREFIXES.stream().noneMatch(requestUri::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String configuredToken = InternalServiceTokenResolver.resolve();
        String requestToken = request.getHeader(InternalServiceTokenResolver.HEADER_NAME);

        if (configuredToken != null && requestToken != null && tokensMatch(configuredToken, requestToken)) {
            CustomUserDetails userDetails = new CustomUserDetails(SYSTEM_PRINCIPAL_UUID, Role.SYSTEM.name());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private boolean tokensMatch(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.trim().getBytes(StandardCharsets.UTF_8),
                actual.trim().getBytes(StandardCharsets.UTF_8)
        );
    }
}
