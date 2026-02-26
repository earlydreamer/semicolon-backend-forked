package dukku.common.global.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class RequestAuthorizationHeaderResolverTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Authorization 헤더가 Bearer 접두어를 포함하면 그대로 반환한다")
    void returnsBearerHeaderAsIs() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-value");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String resolved = RequestAuthorizationHeaderResolver.resolve();

        assertThat(resolved).isEqualTo("Bearer token-value");
    }

    @Test
    @DisplayName("Authorization 헤더에 Bearer 접두어가 없으면 자동으로 추가한다")
    void addsBearerPrefixWhenMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "token-value");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String resolved = RequestAuthorizationHeaderResolver.resolve();

        assertThat(resolved).isEqualTo("Bearer token-value");
    }

    @Test
    @DisplayName("Authorization 헤더 앞뒤 공백은 제거 후 Bearer 포맷으로 반환한다")
    void trimsAuthorizationHeaderBeforeNormalization() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "   token-value   ");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String resolved = RequestAuthorizationHeaderResolver.resolve();

        assertThat(resolved).isEqualTo("Bearer token-value");
    }
}
