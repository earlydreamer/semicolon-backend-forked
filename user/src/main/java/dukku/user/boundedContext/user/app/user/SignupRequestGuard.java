package dukku.user.boundedContext.user.app.user;

import dukku.common.shared.user.exception.UserSignupRequestInProgressException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SignupRequestGuard {

    private static final String SIGNUP_LOCK_KEY_PREFIX = "user:signup:lock:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${custom.signup.request-lock-ttl-seconds:10}")
    private long lockTtlSeconds;

    public String acquire(String email) {
        String normalizedEmail = normalizeEmail(email);
        String lockKey = lockKey(normalizedEmail);
        String lockToken = UUID.randomUUID().toString();

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockToken, Duration.ofSeconds(lockTtlSeconds));

        if (!Boolean.TRUE.equals(acquired)) {
            throw new UserSignupRequestInProgressException();
        }

        return lockToken;
    }

    public void release(String email, String lockToken) {
        String normalizedEmail = normalizeEmail(email);
        String lockKey = lockKey(normalizedEmail);
        Object currentToken = redisTemplate.opsForValue().get(lockKey);

        if (currentToken != null && lockToken.equals(currentToken.toString())) {
            redisTemplate.delete(lockKey);
        }
    }

    private String lockKey(String normalizedEmail) {
        return SIGNUP_LOCK_KEY_PREFIX + normalizedEmail;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
