package dukku.auth.global.config;

import dukku.auth.boundedContext.auth.infra.GoogleOAuth2FailureHandler;
import dukku.auth.boundedContext.auth.infra.GoogleOAuth2SuccessHandler;
import dukku.common.global.auth.internal.InternalServiceAuthenticationFilter;
import dukku.common.global.auth.jwt.JwtAuthenticationFilter;
import dukku.common.global.security.SecurityWhitelist;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile({"dev", "test"})  // 개발 환경 + 테스트 환경
public class SecurityDevConfig {
    @Value("${custom.security.cors.allowed-origins}")
    private String[] allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final InternalServiceAuthenticationFilter internalServiceAuthenticationFilter;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;
    private final GoogleOAuth2FailureHandler googleOAuth2FailureHandler;
    private final OAuth2AuthorizationRequestResolver googleAuthorizationRequestResolver;

    /**
     * CSRF는 서버가 브라우저의 세션/쿠키를 신뢰할 때 공격 위험이 생김.
     * JWT는 Authorization 헤더에 직접 담기 때문에 쿠키 자동 전송과 무관 → CSRF 공격 불가능.
     * REST API + JWT 조합은 주로 비동기 호출(fetch, axios) 사용.
     * 브라우저 폼 기반 요청이 아니므로 CSRF 보호 대상 아님.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                /*
                  RESTful API는 무상태(stateless) 원칙
                  JWT 기반 인증에서는 서버가 상태(session)를 보존하지 않음 → 클라이언트가 JWT를 매 요청마다 전송
                  그러므로 세션은 필요없음
                 */
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityWhitelist.SYSTEM_INTERNAL).hasRole("SYSTEM")
                        .anyRequest().permitAll() // 개발 환경: 모든 요청 허용
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(googleAuthorizationRequestResolver))
                        .successHandler(googleOAuth2SuccessHandler)
                        .failureHandler(googleOAuth2FailureHandler))
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    corsConfig.setAllowedOrigins(
                            Arrays.asList(allowedOrigins)
                    );
                    corsConfig.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
                    corsConfig.setAllowedHeaders(Arrays.asList("Authorization","Content-Type", "Idempotency-Key"));
                    corsConfig.setAllowCredentials(true);
                    return corsConfig;
                }))
                .exceptionHandling(e -> e // 인증 실패시 예외 처리
                        .authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(internalServiceAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
