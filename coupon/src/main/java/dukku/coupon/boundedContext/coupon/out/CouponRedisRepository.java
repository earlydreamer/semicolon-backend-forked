package dukku.coupon.boundedContext.coupon.out;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository // Component -> Repository 권장
@RequiredArgsConstructor
public class CouponRedisRepository {
    private final RedisTemplate<String, String> redisTemplate;

    // Lua Script 관련 코드 삭제됨

    /**
     * 1. 발급 시도 (Lua Script 없이 순수 Redis 명령어 사용)
     * 전략: 일단 감소(DECR) 시키고, 재고가 없으면 다시 증가(INCR)시켜 복구함.
     * 중복 발급은 Set(SADD)을 이용해 체크함.
     *
     * @return 1: 성공, -1: 매진, -2: 중복 발급
     */
    public Long tryIssue(UUID couponUuid, UUID userUuid) {
        String countKey = "coupon:count:" + couponUuid;
        String userSetKey = "coupon:users:" + couponUuid;

        // [Step 1] 중복 발급 검증 (SADD 활용)
        // SADD는 Set에 값이 없을 때만 추가하고 1을 반환, 이미 있으면 0을 반환함.
        // 이 작업은 Atomic 하므로 동시성 이슈 없음.
        Long isNewUser = redisTemplate.opsForSet().add(userSetKey, userUuid.toString());

        if (isNewUser != null && isNewUser == 0) {
            return -2L; // 이미 발급된 유저 (중복)
        }

        // [Step 2] 재고 감소 (DECR 활용)
        // 일단 줄여봅니다. (Atomic)
        Long stock = redisTemplate.opsForValue().decrement(countKey);

        // [Step 3] 재고 확인
        if (stock != null && stock >= 0) {
            return 1L; // 성공 (재고가 0개 이상 남음)
        } else {
            // [Step 4] 실패 시 롤백 (보상 트랜잭션)
            // 재고가 없는데 줄였으므로(-1, -2...), 다시 늘려서 원복해야 함.
            redisTemplate.opsForValue().increment(countKey);
            // 아까 넣었던 유저 Set에서도 제거해야 함.
            redisTemplate.opsForSet().remove(userSetKey, userUuid.toString());

            return -1L; // 매진
        }
    }

    /**
     * 2. DB 저장 실패 시 롤백 (Use Case에서 호출)
     */
    public void rollback(UUID couponUuid, UUID userUuid) {
        String countKey = "coupon:count:" + couponUuid;
        String userSetKey = "coupon:users:" + couponUuid;

        // 재고 +1 복구
        redisTemplate.opsForValue().increment(countKey);
        // 유저 발급 기록(Set) 삭제
        redisTemplate.opsForSet().remove(userSetKey, userUuid.toString());
    }

    // 3. 웜업용 (관리자 도구 등에서 호출)
    public void setLimit(UUID couponUuid, int quantity) {
        redisTemplate.opsForValue().set("coupon:count:" + couponUuid, String.valueOf(quantity));
    }
}