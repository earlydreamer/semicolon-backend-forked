package dukku.semicolon.boundedContext.coupon.app.command;

import dukku.common.global.exception.ConflictException;
import dukku.semicolon.boundedContext.coupon.entity.Coupon;
import dukku.semicolon.boundedContext.coupon.entity.CouponIssueLog;
import dukku.semicolon.boundedContext.coupon.entity.CouponUser;
import dukku.semicolon.boundedContext.coupon.entity.type.IssueResult;
import dukku.semicolon.boundedContext.coupon.out.CouponIssueLogRepository;
import dukku.semicolon.boundedContext.coupon.out.CouponRepository;
import dukku.semicolon.boundedContext.coupon.out.CouponUserRepository;
import dukku.semicolon.shared.coupon.exception.CouponAlreadyExistsException;
import dukku.semicolon.shared.coupon.exception.CouponNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class IssueCouponUseCase {

    private final CouponRepository couponRepository;
    private final CouponUserRepository couponUserRepository;
    private final CouponIssueLogRepository couponIssueLogRepository;

    public void execute(UUID userUuid, UUID couponUuid) {
        LocalDateTime requestedAt = LocalDateTime.now();

        try {
            if (couponUserRepository.existsByUserUuidAndCoupon_Uuid(userUuid, couponUuid)) {
                throw new CouponAlreadyExistsException();
            }

            Coupon coupon = couponRepository.findByUuid(couponUuid)
                    .orElseThrow(CouponNotFoundException::new);

            CouponUser couponUser = CouponUser.issue(userUuid, coupon);
            couponUserRepository.save(couponUser);

            couponIssueLogRepository.save(
                    CouponIssueLog.of(
                            couponUuid,
                            userUuid,
                            IssueResult.SUCCESS,
                            requestedAt
                    )
            );

        } catch (ConflictException e) {

            couponIssueLogRepository.save(
                    CouponIssueLog.of(
                            couponUuid,
                            userUuid,
                            mapResult(e),
                            requestedAt
                    )
            );

            throw e;
        }
    }

    private IssueResult mapResult(ConflictException e) {
        if (e.getMessage().contains("이미")) return IssueResult.DUPLICATE;
        if (e.getMessage().contains("소진")) return IssueResult.SOLD_OUT;
        if (e.getMessage().contains("활성")) return IssueResult.INACTIVE;
        return IssueResult.ERROR;
    }
}