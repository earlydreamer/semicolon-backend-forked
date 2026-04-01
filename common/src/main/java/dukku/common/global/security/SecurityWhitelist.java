package dukku.common.global.security;

public final class SecurityWhitelist {
    public static final String[] COMMON_PUBLIC = {
            "/actuator/health", "/actuator/health/**",

            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs/**",
            "/swagger-config"
    };

    public static final String[] SYSTEM_INTERNAL = {
            "/api/v1/internal/**",
            "/api/v1/products/internal/**",
            "/api/v1/carts/internal/**"
    };

    private SecurityWhitelist() {}
}
