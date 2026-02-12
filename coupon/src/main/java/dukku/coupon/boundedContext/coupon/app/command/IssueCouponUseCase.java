package dukku.coupon.boundedContext.coupon.app.command;

import dukku.common.global.exception.ConflictException;
import dukku.common.shared.coupon.exception.CouponAlreadyExistsException;
import dukku.common.shared.coupon.exception.CouponNotFoundException;
import dukku.common.shared.coupon.exception.CouponSoldOutException;
import dukku.common.shared.coupon.type.IssueResult;
import dukku.coupon.boundedContext.coupon.entity.Coupon;
import dukku.coupon.boundedContext.coupon.entity.CouponUser;
import dukku.coupon.boundedContext.coupon.out.CouponRedisRepository;
import dukku.coupon.boundedContext.coupon.out.CouponRepository;
import dukku.coupon.boundedContext.coupon.out.CouponUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IssueCouponUseCase {
    private final CouponRedisRepository couponRedisRepository;
    private final CouponIssueService couponIssueService;
    private final CouponIssueLogManager couponIssueLogManager;
    private final CouponRepository couponRepository;

    public void execute(UUID userUuid, UUID couponUuid) {
        LocalDateTime requestedAt = LocalDateTime.now();

        // 1. Redis 검증 시도
        Long result = couponRedisRepository.tryIssue(couponUuid, userUuid);

        /**
         * Redis 검증 결과 (Lua Script 반환값)
         * 1 : 발급 가능 (성공)
         * -1 : 수량 소진 (Sold Out)
         * -2 : 중복 발급 (Duplicate)
         * -3 : Redis 내 쿠폰 설정 정보 없음 (Init Required)
         */
        // 2. 만약 Redis에 수량 정보가 없다면? (Warm-up 안 된 경우)
        if (result == -3) {
            initRedisLimit(couponUuid);
            result = couponRedisRepository.tryIssue(couponUuid, userUuid);
        }

        try {
            validateRedisResult(result);
            couponIssueService.saveIssueResult(userUuid, couponUuid, requestedAt);
        } catch (ConflictException e) {
            couponIssueLogManager.record(couponUuid, userUuid, mapResult(e), requestedAt);

            throw e;
        }
    }

    private void initRedisLimit(UUID couponUuid) {
        // DB에서 실제 수량을 가져와서 Redis에 세팅
        Coupon coupon = couponRepository.findByUuid(couponUuid)
                .orElseThrow(CouponNotFoundException::new);

        couponRedisRepository.setLimit(couponUuid, coupon.getTotalQuantity());
    }

    private void validateRedisResult(Long result) {
        if (result == -2) throw new CouponAlreadyExistsException();
        if (result == -1) throw new CouponSoldOutException();
    }

    private IssueResult mapResult(ConflictException e) {
        if (e instanceof CouponAlreadyExistsException) return IssueResult.DUPLICATE;
        if (e instanceof CouponSoldOutException) return IssueResult.SOLD_OUT;
        return IssueResult.ERROR;
    }
}