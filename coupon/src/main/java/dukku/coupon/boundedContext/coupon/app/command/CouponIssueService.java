package dukku.coupon.boundedContext.coupon.app.command;

import dukku.common.shared.coupon.exception.CouponNotFoundException;
import dukku.common.shared.coupon.type.IssueResult;
import dukku.coupon.boundedContext.coupon.entity.Coupon;
import dukku.coupon.boundedContext.coupon.entity.CouponUser;
import dukku.coupon.boundedContext.coupon.out.CouponRepository;
import dukku.coupon.boundedContext.coupon.out.CouponUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponIssueService {
    private final CouponRepository couponRepository;
    private final CouponUserRepository couponUserRepository;
    private final CouponIssueLogManager couponIssueLogManager;

    @Transactional
    public void saveIssueResult(UUID userUuid, UUID couponUuid, LocalDateTime requestedAt) {
        // 1. 단순 정보 참조 (수정 X)
        Coupon coupon = couponRepository.findByUuid(couponUuid)
                .orElseThrow(CouponNotFoundException::new);

        // 2. DB 수량 동기화 (Redis와 맞춤)
        couponRepository.decreaseQuantity(couponUuid);

        // 3. 발급 이력 저장
        CouponUser couponUser = CouponUser.create(userUuid, coupon);
        couponUserRepository.save(couponUser);

        // 4. 성공 로그 기록
        couponIssueLogManager.record(couponUuid, userUuid, IssueResult.SUCCESS, requestedAt);
    }
}