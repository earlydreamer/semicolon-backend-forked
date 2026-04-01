package dukku.common.global.auth.jwt;

import dukku.common.global.auth.detail.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class JwtAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("이미 인증 객체가 있으면 JWT 필터는 기존 인증을 덮어쓰지 않는다")
    void doesNotOverrideExistingAuthentication() throws Exception {
        JwtTokenUtil jwtTokenUtil = mock(JwtTokenUtil.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenUtil);

        CustomUserDetails userDetails = new CustomUserDetails(UUID.randomUUID(), "SYSTEM");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/internal/orders/items/confirmed");
        request.addHeader("Authorization", "Bearer user-jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        verifyNoInteractions(jwtTokenUtil);
    }
}
