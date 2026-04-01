package dukku.common.global.auth.internal;

import dukku.common.global.UserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class InternalServiceAuthenticationFilterTest {

    private static final String PROPERTY_NAME = "INTERNAL_SERVICE_TOKEN";

    private final InternalServiceAuthenticationFilter filter = new InternalServiceAuthenticationFilter();

    @AfterEach
    void tearDown() {
        System.clearProperty(PROPERTY_NAME);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("internal 경로에서 X-Internal-Service-Token이 일치하면 SYSTEM 인증을 세팅한다")
    void authenticatesSystemRequestWithMatchingToken() throws Exception {
        System.setProperty(PROPERTY_NAME, "internal-token");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/internal/orders/items/confirmed");
        request.addHeader("X-Internal-Service-Token", "internal-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(UserUtil.getRole()).isEqualTo("ROLE_SYSTEM");
    }

    @Test
    @DisplayName("internal 경로에서 토큰이 불일치하면 인증을 세팅하지 않는다")
    void leavesSecurityContextEmptyWhenTokenDoesNotMatch() throws Exception {
        System.setProperty(PROPERTY_NAME, "internal-token");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/internal/orders/items/confirmed");
        request.addHeader("X-Internal-Service-Token", "wrong-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
