package dukku.user.boundedContext.user.app.user;

import dukku.common.shared.user.dto.UserRegisterRequest;
import dukku.common.shared.user.exception.UserIdempotencyKeyRequiredException;
import dukku.common.shared.user.exception.UserSignupHashingFailedException;
import dukku.common.shared.user.exception.UserSignupIdempotencyConflictException;
import dukku.common.shared.user.exception.UserSignupRequestInProgressException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class SignupIdempotencyService {

    private static final String SIGNUP_IDEMPOTENCY_KEY_PREFIX = "user:signup:idempotency:";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${custom.signup.idempotency.in-progress-ttl-seconds:30}")
    private long inProgressTtlSeconds;

    @Value("${custom.signup.idempotency.completed-ttl-seconds:1800}")
    private long completedTtlSeconds;

    public SignupIdempotencyContext begin(String idempotencyKey, UserRegisterRequest request) {
        String normalizedKey = normalizeKey(idempotencyKey);
        String requestHash = requestHash(request);
        String redisKey = redisKey(normalizedKey);
        String inProgressValue = toValue(STATUS_IN_PROGRESS, requestHash);

        for (int attempt = 0; attempt < 2; attempt++) {
            Object existing = redisTemplate.opsForValue().get(redisKey);
            if (existing == null) {
                Boolean acquired = redisTemplate.opsForValue()
                        .setIfAbsent(redisKey, inProgressValue, Duration.ofSeconds(inProgressTtlSeconds));
                if (Boolean.TRUE.equals(acquired)) {
                    return SignupIdempotencyContext.started(redisKey, requestHash);
                }
                continue;
            }
            return resolveExisting(redisKey, existing.toString(), requestHash);
        }

        throw new UserSignupRequestInProgressException();
    }

    public void markCompleted(SignupIdempotencyContext context) {
        if (context.isAlreadyCompleted()) {
            return;
        }
        String value = toValue(STATUS_COMPLETED, context.getRequestHash());
        redisTemplate.opsForValue().set(
                context.getRedisKey(),
                value,
                Duration.ofSeconds(completedTtlSeconds)
        );
    }

    public void rollback(SignupIdempotencyContext context) {
        if (context.isAlreadyCompleted()) {
            return;
        }
        String expected = toValue(STATUS_IN_PROGRESS, context.getRequestHash());
        Object current = redisTemplate.opsForValue().get(context.getRedisKey());
        if (current != null && expected.equals(current.toString())) {
            redisTemplate.delete(context.getRedisKey());
        }
    }

    private SignupIdempotencyContext resolveExisting(String redisKey, String raw, String requestHash) {
        String[] parts = raw.split(":", 2);
        if (parts.length != 2) {
            throw new UserSignupRequestInProgressException();
        }
        String status = parts[0];
        String existingHash = parts[1];
        if (!existingHash.equals(requestHash)) {
            throw new UserSignupIdempotencyConflictException();
        }
        if (STATUS_COMPLETED.equals(status)) {
            return SignupIdempotencyContext.completed(redisKey, requestHash);
        }
        throw new UserSignupRequestInProgressException();
    }

    private String requestHash(UserRegisterRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        String nickname = request.getNickname() == null ? "" : request.getNickname().trim();
        String password = request.getPassword() == null ? "" : request.getPassword();
        String payload = email + "|" + nickname + "|" + password;
        return sha256Hex(payload);
    }

    private String normalizeKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new UserIdempotencyKeyRequiredException();
        }
        return idempotencyKey.trim();
    }

    private String redisKey(String normalizedKey) {
        return SIGNUP_IDEMPOTENCY_KEY_PREFIX + normalizedKey;
    }

    private String toValue(String status, String requestHash) {
        return status + ":" + requestHash;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new UserSignupHashingFailedException();
        }
    }

    @Getter
    public static class SignupIdempotencyContext {
        private final String redisKey;
        private final String requestHash;
        private final boolean alreadyCompleted;

        private SignupIdempotencyContext(String redisKey, String requestHash, boolean alreadyCompleted) {
            this.redisKey = redisKey;
            this.requestHash = requestHash;
            this.alreadyCompleted = alreadyCompleted;
        }

        static SignupIdempotencyContext started(String redisKey, String requestHash) {
            return new SignupIdempotencyContext(redisKey, requestHash, false);
        }

        static SignupIdempotencyContext completed(String redisKey, String requestHash) {
            return new SignupIdempotencyContext(redisKey, requestHash, true);
        }
    }
}
