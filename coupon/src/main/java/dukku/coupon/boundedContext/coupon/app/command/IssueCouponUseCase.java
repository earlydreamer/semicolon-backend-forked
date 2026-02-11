package dukku.coupon.boundedContext.coupon.app.command;

import dukku.common.global.exception.ConflictException;
import dukku.common.shared.coupon.type.IssueResult;
import dukku.coupon.boundedContext.coupon.entity.Coupon;
import dukku.coupon.boundedContext.coupon.entity.CouponUser;
import dukku.coupon.boundedContext.coupon.out.CouponRepository;
import dukku.coupon.boundedContext.coupon.out.CouponUserRepository;
import dukku.coupon.shared.coupon.exception.CouponAlreadyExistsException;
import dukku.coupon.shared.coupon.exception.CouponNotFoundException;
import dukku.coupon.shared.coupon.exception.CouponSoldOutException;
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
    private final CouponIssueLogManager couponIssueLogManager;

    public void execute(UUID userUuid, UUID couponUuid) {
        LocalDateTime requestedAt = LocalDateTime.now();

        try {
            // 1. 중복 체크
            if (couponUserRepository.existsByUserUuidAndCoupon_Uuid(userUuid, couponUuid)) {
                throw new CouponAlreadyExistsException();
            }

            // 2. DB 원자적 업데이트 (여기서 100개까지 순차적으로 성공함)
            int result = couponRepository.decreaseQuantity(couponUuid);
            if (result == 0) {
                throw new CouponSoldOutException();
            }

            // 3. Coupon 엔티티는 단순 정보 참조용으로만 사용 (수정 X)
            Coupon coupon = couponRepository.findByUuid(couponUuid)
                    .orElseThrow(CouponNotFoundException::new);

            // 4. 이력 저장 (내부에서 coupon.issue() 호출 금지)
            CouponUser couponUser = CouponUser.create(userUuid, coupon);
            couponUserRepository.save(couponUser);

            couponIssueLogManager.record(couponUuid, userUuid, IssueResult.SUCCESS, requestedAt);

        } catch (ConflictException e) {
            couponIssueLogManager.record(couponUuid, userUuid, mapResult(e), requestedAt);
            throw e;
        }
    }

    private IssueResult mapResult(ConflictException e) {
        if (e.getMessage().contains("이미")) return IssueResult.DUPLICATE;
        if (e.getMessage().contains("소진")) return IssueResult.SOLD_OUT;
        return IssueResult.ERROR;
    }
}