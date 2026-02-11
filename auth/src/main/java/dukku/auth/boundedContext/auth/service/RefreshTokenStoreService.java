package dukku.auth.boundedContext.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenStoreService {
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:";

    private final RedisTemplate<String, Object> redisTemplate;

    public void save(UUID userUuid, String refreshToken, Duration ttl) {
        if (userUuid == null || refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        redisTemplate.opsForValue().set(buildKey(userUuid), refreshToken, ttl);
    }

    public String get(UUID userUuid) {
        if (userUuid == null) {
            return null;
        }
        Object stored = redisTemplate.opsForValue().get(buildKey(userUuid));
        return stored == null ? null : stored.toString();
    }

    public void delete(UUID userUuid) {
        if (userUuid == null) {
            return;
        }
        redisTemplate.delete(buildKey(userUuid));
    }

    private String buildKey(UUID userUuid) {
        return REFRESH_TOKEN_KEY_PREFIX + userUuid;
    }
}
