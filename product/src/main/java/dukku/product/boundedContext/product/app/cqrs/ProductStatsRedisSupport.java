package dukku.product.boundedContext.product.app.cqrs;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProductStatsRedisSupport {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String DIRTY_KEY = "product:stats:dirty";
    private static final String VIEW_KEY_PREFIX = "product:stats:view:";
    private static final String LIKE_KEY_PREFIX = "product:stats:like:";
    private static final String COMMENT_KEY_PREFIX = "product:stats:comment:";

    // 좋아요 수를 1 증가
    public void incrementLike(int productId) {
        redisTemplate.opsForValue().increment(LIKE_KEY_PREFIX + productId);
        markDirty(String.valueOf(productId));
    }

    // 좋아요 수를 1 감소
    public void decrementLike(int productId) {
        redisTemplate.opsForValue().decrement(LIKE_KEY_PREFIX + productId);
        markDirty(String.valueOf(productId));
    }

    // 댓글 수를 1 증가
    public void incrementComment(int productId) {
        redisTemplate.opsForValue().increment(COMMENT_KEY_PREFIX + productId);
        markDirty(String.valueOf(productId));
    }

    // 댓글 수를 1 감소
    public void decrementComment(int productId) {
        redisTemplate.opsForValue().decrement(COMMENT_KEY_PREFIX + productId);
        markDirty(String.valueOf(productId));
    }

    // 조회수 1 증가
    public void incrementView(int productId) {
        redisTemplate.opsForValue().increment(VIEW_KEY_PREFIX + productId);
        markDirty(String.valueOf(productId));
    }

    // 동기화가 필요한 상품 ID를 dirty 집합에 기록
    private void markDirty(String productId) {
        redisTemplate.opsForSet().add(DIRTY_KEY, productId);
    }

    // Redis에서 현재 좋아요 카운트를 조회
    public Long getLikeCount(int productId) {
        Object value = redisTemplate.opsForValue().get(LIKE_KEY_PREFIX + productId);
        if (value == null) {
            return null;
        }
        return parseLongSafe(value);
    }

    // Redis에서 현재 조회수를 조회
    public Long getViewCount(int productId) {
        Object value = redisTemplate.opsForValue().get(VIEW_KEY_PREFIX + productId);
        if (value == null) {
            return null;
        }
        return parseLongSafe(value);
    }

    // Redis에서 현재 댓글 수를 조회
    public Long getCommentCount(int productId) {
        Object value = redisTemplate.opsForValue().get(COMMENT_KEY_PREFIX + productId);
        if (value == null) {
            return null;
        }
        return parseLongSafe(value);
    }

    // 동기화 대기 중인 상품 ID 목록 조회
    public Set<Object> getDirtyProductIds() {
        return redisTemplate.opsForSet().members(DIRTY_KEY);
    }

    // 여러 상품의 view/like/comment 카운트를 한 번에 조회
    public List<Object> getMultiStats(List<Integer> productIds) {
        List<String> keys = new ArrayList<>();
        for (Integer id : productIds) {
            keys.add(VIEW_KEY_PREFIX + id);
            keys.add(LIKE_KEY_PREFIX + id);
            keys.add(COMMENT_KEY_PREFIX + id);
        }

        return redisTemplate.opsForValue().multiGet(keys);
    }

    public void cleanupDirtyIds(Set<Object> processedIds) {
        redisTemplate.opsForSet().remove(DIRTY_KEY, processedIds.toArray());
    }

    // Redis 값 타입이 Integer/Long/String 등으로 들어와도 안전하게 Long으로 변환
    public long parseLongSafe(Object value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
