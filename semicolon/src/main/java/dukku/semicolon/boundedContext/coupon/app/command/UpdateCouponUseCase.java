package dukku.semicolon.boundedContext.coupon.app.command;

import dukku.common.global.exception.NotFoundException;
import dukku.semicolon.boundedContext.coupon.entity.Coupon;
import dukku.semicolon.boundedContext.coupon.out.CouponRepository;
import dukku.semicolon.shared.coupon.dto.CouponUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class UpdateCouponUseCase {
    private final CouponRepository couponRepository;

    public void execute(UUID couponUuid, CouponUpdateRequest request) {
        Coupon coupon = couponRepository.findByUuid(couponUuid)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 쿠폰입니다."));

        coupon.updateDraft(request);
    }
}
