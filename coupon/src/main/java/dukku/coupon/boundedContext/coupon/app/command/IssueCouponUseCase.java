package dukku.coupon.boundedContext.coupon.app.command;

import dukku.common.global.exception.ConflictException;
import dukku.common.shared.coupon.exception.CouponNotFoundException;
import dukku.common.shared.coupon.type.IssueResult;
import dukku.coupon.boundedContext.coupon.entity.Coupon;
import dukku.coupon.boundedContext.coupon.out.CouponRedisRepository;
import dukku.coupon.boundedContext.coupon.out.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class IssueCouponUseCase {
    private final CouponRedisRepository couponRedisRepository;
    private final CouponIssueService couponIssueService;
    private final CouponIssueLogManager couponIssueLogManager;
    private final CouponRepository couponRepository;

    private final Map<UUID, Coupon> couponMetadataCache = new ConcurrentHashMap<>();

    // 쿠폰 메타데이터를 조회하고 Redis를 통해 선착순 검증을 수행합니다.
    public void execute(UUID userUuid, UUID couponUuid) {
        LocalDateTime requestedAt = LocalDateTime.now();

        Coupon coupon = getCachedCoupon(couponUuid);

        IssueResult result = couponRedisRepository.tryIssue(couponUuid, userUuid);

        if (result == IssueResult.SUCCESS) {
            issueCoupon(userUuid, coupon, requestedAt);
        } else {
            handleFailure(result, userUuid, couponUuid, requestedAt);
        }
    }

    // 검증을 통과한 요청에 한해 DB에 발급 이력을 저장하며, 실패 시 Redis 데이터를 롤백합니다.
    private void issueCoupon(UUID userUuid, Coupon coupon, LocalDateTime requestedAt) {
        try {
            couponIssueService.saveIssueResult(userUuid, coupon, requestedAt);
        } catch (Exception e) {
            log.error("DB 저장 실패로 인한 Redis 롤백 수행. User: {}, Coupon: {}", userUuid, coupon.getUuid());
            couponRedisRepository.rollback(coupon.getUuid(), userUuid);

            throw e;
        }
    }

    // 발급 실패 사유를 비동기로 로깅하고 클라이언트에게 명확한 예외를 반환합니다.
    private void handleFailure(IssueResult result, UUID userUuid, UUID couponUuid, LocalDateTime requestedAt) {
        couponIssueLogManager.record(couponUuid, userUuid, result, requestedAt);

        String message = (result == IssueResult.DUPLICATE) ? "이미 발급된 쿠폰입니다." : "선착순 마감되었습니다.";
        throw new ConflictException(message);
    }

    // 쿠폰 메타데이터를 로컬 캐시에 저장하여 DB 조회 부하를 최소화합니다.
    private Coupon getCachedCoupon(UUID couponUuid) {
        return couponMetadataCache.computeIfAbsent(couponUuid, k ->
                couponRepository.findByUuid(k)
                        .orElseThrow(CouponNotFoundException::new)
        );
    }
}