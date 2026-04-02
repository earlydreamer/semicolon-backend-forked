package dukku.auth.boundedContext.auth.controller;

import dukku.auth.boundedContext.auth.infra.GoogleOAuth2FailureHandler;
import dukku.auth.boundedContext.auth.infra.GoogleOAuth2SuccessHandler;
import dukku.auth.boundedContext.auth.service.AuthService;
import dukku.auth.global.config.SecurityReleaseConfig;
import dukku.common.global.auth.InternalServiceTokenResolver;
import dukku.common.global.auth.internal.InternalServiceAuthenticationFilter;
import dukku.common.global.auth.jwt.JwtAuthenticationFilter;
import dukku.common.global.auth.jwt.error.CustomAuthenticationEntryPoint;
import dukku.common.global.auth.jwt.JwtTokenUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringJUnitConfig(AuthInternalControllerSecurityTest.TestConfig.class)
@WebAppConfiguration
@ActiveProfiles("release")
@TestPropertySource(properties = "custom.security.cors.allowed-origins=http://localhost:3000")
class AuthInternalControllerSecurityTest {

    private static final UUID USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(authService);
        mockMvc = webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(InternalServiceTokenResolver.PROPERTY_NAME);
    }

    @Test
    @DisplayName("릴리즈 프로필에서 내부 토큰 없이 auth 세션 폐기 엔드포인트를 호출하면 401을 반환한다")
    void rejectsRequestWithoutInternalServiceToken() throws Exception {
        System.setProperty(InternalServiceTokenResolver.PROPERTY_NAME, "internal-token");

        mockMvc.perform(delete("/api/v1/internal/auth/users/{userUuid}/sessions", USER_UUID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("릴리즈 프로필에서 올바른 내부 토큰으로 auth 세션 폐기 엔드포인트를 호출하면 204를 반환한다")
    void allowsRequestWithInternalServiceToken() throws Exception {
        System.setProperty(InternalServiceTokenResolver.PROPERTY_NAME, "internal-token");

        mockMvc.perform(delete("/api/v1/internal/auth/users/{userUuid}/sessions", USER_UUID)
                        .header(InternalServiceTokenResolver.HEADER_NAME, "internal-token"))
                .andExpect(status().isNoContent());

        verify(authService).revokeAllSessions(USER_UUID);
    }

    @Configuration
    @EnableWebMvc
    @Import({SecurityReleaseConfig.class, InternalServiceAuthenticationFilter.class})
    static class TestConfig {

        @Bean
        AuthService authService() {
            return Mockito.mock(AuthService.class);
        }

        @Bean
        AuthInternalController authInternalController(AuthService authService) {
            return new AuthInternalController(authService);
        }

        @Bean
        JwtTokenUtil jwtTokenUtil() {
            return Mockito.mock(JwtTokenUtil.class);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil) {
            return new JwtAuthenticationFilter(jwtTokenUtil);
        }

        @Bean
        AuthenticationEntryPoint authenticationEntryPoint() {
            return new CustomAuthenticationEntryPoint();
        }

        @Bean
        GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler() {
            return Mockito.mock(GoogleOAuth2SuccessHandler.class);
        }

        @Bean
        GoogleOAuth2FailureHandler googleOAuth2FailureHandler() {
            return Mockito.mock(GoogleOAuth2FailureHandler.class);
        }

        @Bean
        OAuth2AuthorizationRequestResolver googleAuthorizationRequestResolver() {
            return Mockito.mock(OAuth2AuthorizationRequestResolver.class);
        }

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return Mockito.mock(ClientRegistrationRepository.class);
        }
    }
}
