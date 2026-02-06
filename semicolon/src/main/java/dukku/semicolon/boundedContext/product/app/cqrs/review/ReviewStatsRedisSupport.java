package dukku.semicolon.boundedContext.product.app.cqrs.review;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReviewStatsRedisSupport {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String DIRTY_KEY = "seller:review:dirty";
    private static final String COUNT_KEY_PREFIX = "seller:review:count:";
    private static final String RATING_SUM_KEY_PREFIX = "seller:review:rating_sum:";

    // ===== write =====

    public void incrementReviewCount(UUID sellerUuid) {
        redisTemplate.opsForValue().increment(COUNT_KEY_PREFIX + sellerUuid);
        markDirty(sellerUuid);
    }

    public void decrementReviewCount(UUID sellerUuid) {
        redisTemplate.opsForValue().decrement(COUNT_KEY_PREFIX + sellerUuid);
        markDirty(sellerUuid);
    }

    public void addRating(UUID sellerUuid, int rating) {
        redisTemplate.opsForValue().increment(RATING_SUM_KEY_PREFIX + sellerUuid, rating);
        markDirty(sellerUuid);
    }

    public void subtractRating(UUID sellerUuid, int rating) {
        redisTemplate.opsForValue().increment(RATING_SUM_KEY_PREFIX + sellerUuid, -rating);
        markDirty(sellerUuid);
    }

    private void markDirty(UUID sellerUuid) {
        redisTemplate.opsForSet().add(DIRTY_KEY, sellerUuid.toString());
    }

    // ===== read =====

    public Set<String> getDirtySellerUuids() {
        Set<Object> raw = redisTemplate.opsForSet().members(DIRTY_KEY);
        if (raw == null) return Set.of();
        return raw.stream().map(String::valueOf).collect(Collectors.toSet());
    }

    public long getReviewCount(UUID sellerUuid) {
        Object v = redisTemplate.opsForValue().get(COUNT_KEY_PREFIX + sellerUuid);
        return parseLongSafe(v);
    }

    public long getRatingSum(UUID sellerUuid) {
        Object v = redisTemplate.opsForValue().get(RATING_SUM_KEY_PREFIX + sellerUuid);
        return parseLongSafe(v);
    }

    // ===== cleanup =====

    public void cleanupDirty(Set<String> ids) {
        redisTemplate.opsForSet().remove(DIRTY_KEY, ids.toArray());
    }

    private long parseLongSafe(Object value) {
        if (value == null) return 0;
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }
}
