package dukku.common.global.ratelimit;

public enum RateLimitScope {
    IP,           // IP 기반 제한
    USER,         // 사용자 기반 제한
    USER_OR_IP    // 사용자 또는 IP 기반 제한
}
