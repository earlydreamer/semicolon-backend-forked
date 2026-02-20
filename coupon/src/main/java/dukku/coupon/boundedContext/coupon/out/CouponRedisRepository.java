package dukku.coupon.boundedContext.coupon.out;

import dukku.common.shared.coupon.type.IssueResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CouponRedisRepository {
    private final RedisTemplate<String, String> redisTemplate;

    // 순수 Redis 명령어를 사용하여 원자적으로 쿠폰 발급 가능 여부를 검증합니다.
    public IssueResult tryIssue(UUID couponUuid, UUID userUuid) {
        String countKey = "coupon:count:" + couponUuid;
        String userSetKey = "coupon:users:" + couponUuid;

        // SADD 명령어를 통해 유저의 중복 발급 여부를 확인합니다.
        Long isNewUser = redisTemplate.opsForSet().add(userSetKey, userUuid.toString());

        if (isNewUser != null && isNewUser == 0) {
            return IssueResult.DUPLICATE;
        }

        // DECR 명령어를 통해 재고를 1 차감합니다.
        Long stock = redisTemplate.opsForValue().decrement(countKey);

        // 차감된 재고가 0 이상이면 선착순 조건에 부합하여 발급 성공으로 판단합니다.
        if (stock != null && stock >= 0) {
            return IssueResult.SUCCESS;
        } else {
            // 재고가 부족한 경우 차감된 재고와 등록된 유저 정보를 원상 복구합니다.
            redisTemplate.opsForValue().increment(countKey);
            redisTemplate.opsForSet().remove(userSetKey, userUuid.toString());

            return IssueResult.SOLD_OUT;
        }
    }

    // DB 저장 실패 등 예외 발생 시 Redis의 재고와 유저 발급 이력을 롤백합니다.
    public void rollback(UUID couponUuid, UUID userUuid) {
        String countKey = "coupon:count:" + couponUuid;
        String userSetKey = "coupon:users:" + couponUuid;

        redisTemplate.opsForValue().increment(countKey);
        redisTemplate.opsForSet().remove(userSetKey, userUuid.toString());
    }

    // 초기 쿠폰의 총 재고 수량을 Redis에 설정합니다.
    public void setLimit(UUID couponUuid, int quantity) {
        redisTemplate.opsForValue().set("coupon:count:" + couponUuid, String.valueOf(quantity));
    }
}