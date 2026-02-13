package dukku.coupon.boundedContext.coupon.app.command;

import dukku.common.global.exception.ConflictException;
import dukku.common.shared.coupon.exception.CouponNotFoundException;
import dukku.common.shared.coupon.type.IssueResult;
import dukku.coupon.boundedContext.coupon.entity.Coupon;
import dukku.coupon.boundedContext.coupon.out.CouponRedisRepository;
import dukku.coupon.boundedContext.coupon.out.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class IssueCouponUseCase {
    private final CouponRedisRepository couponRedisRepository;
    private final CouponIssueService couponIssueService;
    private final CouponIssueLogManager couponIssueLogManager;
    private final CouponRepository couponRepository;

    // 쿠폰 정보를 메모리에 캐싱하여 DB 부하 제거
    private final Map<UUID, Coupon> couponCache = new ConcurrentHashMap<>();

    public void execute(UUID userUuid, UUID couponUuid) {
        LocalDateTime requestedAt = LocalDateTime.now();
        Long result = couponRedisRepository.tryIssue(couponUuid, userUuid);

        if (result == -3) {
            initRedisLimit(couponUuid);
            result = couponRedisRepository.tryIssue(couponUuid, userUuid);
        }

        if (result <= 0) {
            IssueResult failResult = (result == -2) ? IssueResult.DUPLICATE : IssueResult.SOLD_OUT;
            couponIssueLogManager.record(couponUuid, userUuid, failResult, requestedAt);
            throw new ConflictException("발급 실패: " + failResult);
        }

        // 캐시에서 조회 후 없으면 DB 조회 (Double Check)
        Coupon coupon = couponCache.computeIfAbsent(couponUuid, k ->
                couponRepository.findByUuid(k).orElseThrow(CouponNotFoundException::new)
        );

        couponIssueService.saveIssueResult(userUuid, coupon, requestedAt);
    }

    private void initRedisLimit(UUID couponUuid) {
        Coupon coupon = couponRepository.findByUuid(couponUuid)
                .orElseThrow(CouponNotFoundException::new);
        couponRedisRepository.setLimit(couponUuid, coupon.getTotalQuantity());
    }
}