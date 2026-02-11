package dukku.coupon.boundedContext.coupon.app.command;

import dukku.common.shared.coupon.dto.CouponCreateRequest;
import dukku.common.shared.coupon.dto.CouponResponse;
import dukku.coupon.boundedContext.coupon.entity.Coupon;
import dukku.coupon.boundedContext.coupon.out.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CreateCouponUseCase {

    private final CouponRepository couponRepository;

    public CouponResponse execute(CouponCreateRequest request) {
        Coupon coupon = Coupon.createCoupon(request);
        couponRepository.save(coupon);

        return Coupon.from(coupon);
    }
}
