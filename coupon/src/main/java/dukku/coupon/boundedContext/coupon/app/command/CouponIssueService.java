package dukku.coupon.boundedContext.coupon.app.command;

import dukku.common.shared.coupon.type.IssueResult;
import dukku.coupon.boundedContext.coupon.entity.Coupon;
import dukku.coupon.boundedContext.coupon.entity.CouponUser;
import dukku.coupon.boundedContext.coupon.out.CouponUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponIssueService {
    private final CouponUserRepository couponUserRepository;
    private final CouponIssueLogManager couponIssueLogManager;

    @Transactional
    public void saveIssueResult(UUID userUuid, Coupon coupon, LocalDateTime requestedAt) {
        CouponUser couponUser = CouponUser.create(userUuid, coupon);
        couponUserRepository.save(couponUser);

        couponIssueLogManager.record(coupon.getUuid(), userUuid, IssueResult.SUCCESS, requestedAt);
    }
}