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

    // 쿠폰 메타데이터 캐싱 (재고 수량은 캐싱하지 않고 Redis/DB를 믿어야 함)
    private final Map<UUID, Coupon> couponMetadataCache = new ConcurrentHashMap<>();

    public void execute(UUID userUuid, UUID couponUuid) {
        LocalDateTime requestedAt = LocalDateTime.now();

        // 1. 쿠폰 메타데이터 조회
        Coupon coupon = getCachedCoupon(couponUuid);

        // 2. Redis를 통한 검증 및 수량 차감 (변경된 Repository 호출)
        // 내부적으로 Lua 대신 DECR/SADD를 사용하지만, 호출하는 쪽은 모름 (추상화)
        Long result = couponRedisRepository.tryIssue(couponUuid, userUuid);

        // 3. 결과 처리
        if (result == 1) {
            issueCoupon(userUuid, coupon, requestedAt);
        } else {
            handleFailure(result, userUuid, couponUuid, requestedAt);
        }
    }

    private void issueCoupon(UUID userUuid, Coupon coupon, LocalDateTime requestedAt) {
        try {
            // 3-1. DB에 발급 내역 저장
            couponIssueService.saveIssueResult(userUuid, coupon, requestedAt);

        } catch (Exception e) {
            // [CRITICAL] DB 저장이 실패했다면? Redis 재고를 다시 원복해야 함!
            log.error("DB 저장 실패로 인한 Redis 롤백 수행. User: {}, Coupon: {}", userUuid, coupon.getUuid());
            couponRedisRepository.rollback(coupon.getUuid(), userUuid);
            throw e;
        }
    }

    private void handleFailure(Long result, UUID userUuid, UUID couponUuid, LocalDateTime requestedAt) {
        IssueResult failResult = (result == -2) ? IssueResult.DUPLICATE : IssueResult.SOLD_OUT;

        // 실패 로그 기록 (비동기)
        couponIssueLogManager.record(couponUuid, userUuid, failResult, requestedAt);

        // 클라이언트에게 명확한 에러 반환
        String message = (failResult == IssueResult.DUPLICATE) ? "이미 발급된 쿠폰입니다." : "선착순 마감되었습니다.";
        throw new ConflictException(message);
    }

    private Coupon getCachedCoupon(UUID couponUuid) {
        return couponMetadataCache.computeIfAbsent(couponUuid, k ->
                couponRepository.findByUuid(k)
                        .orElseThrow(CouponNotFoundException::new)
        );
    }
}