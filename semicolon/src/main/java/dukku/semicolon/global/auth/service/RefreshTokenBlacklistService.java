package dukku.semicolon.global.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenBlacklistService {
    private static final String REFRESH_BLACKLIST_KEY_PREFIX = "blacklist:refresh:";

    private final RedisTemplate<String, Object> redisTemplate;

    public boolean isBlacklisted(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return false;
        }
        return redisTemplate.opsForValue().get(buildKey(refreshToken)) != null;
    }

    public void blacklist(String refreshToken, Duration ttl) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        redisTemplate.opsForValue().set(buildKey(refreshToken), "true", ttl);
    }

    private String buildKey(String refreshToken) {
        return REFRESH_BLACKLIST_KEY_PREFIX + refreshToken;
    }
}
