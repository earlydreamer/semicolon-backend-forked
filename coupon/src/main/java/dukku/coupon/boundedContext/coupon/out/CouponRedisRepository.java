package dukku.coupon.boundedContext.coupon.out;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CouponRedisRepository {
    private final RedisTemplate<String, String> redisTemplate;

    // KEY 1: 카운터, KEY 2: 유저목록, KEY 3: 캐싱된 수량 정보
    private static final RedisScript<Long> ISSUE_SCRIPT = RedisScript.of(
            new ClassPathResource("scripts/issue-coupon.lua"),
            Long.class
    );

    public Long tryIssue(UUID couponUuid, UUID userUuid) {
        return redisTemplate.execute(
                ISSUE_SCRIPT,
                List.of(
                        "coupon:count:" + couponUuid,
                        "coupon:users:" + couponUuid,
                        "coupon:limit:" + couponUuid
                ),
                userUuid.toString()
        );
    }

    // 관리자 페이지나 이벤트 시작 전 호출하여 수량을 Redis에 미리 세팅
    public void setLimit(UUID couponUuid, int totalQuantity) {
        redisTemplate.opsForValue().set("coupon:limit:" + couponUuid, String.valueOf(totalQuantity));
    }
}
