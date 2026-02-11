package dukku.coupon.boundedContext.coupon.app.command;

import dukku.common.shared.coupon.dto.CouponUpdateRequest;
import dukku.common.shared.coupon.exception.CouponNotFoundException;
import dukku.coupon.boundedContext.coupon.entity.Coupon;
import dukku.coupon.boundedContext.coupon.out.CouponRepository;
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
                .orElseThrow(CouponNotFoundException::new);

        coupon.updateDraft(request);
    }
}
