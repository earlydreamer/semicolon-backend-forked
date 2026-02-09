package dukku.semicolon.boundedContext.coupon.app.command;

import dukku.semicolon.boundedContext.coupon.entity.Coupon;
import dukku.semicolon.boundedContext.coupon.out.CouponRepository;
import dukku.semicolon.shared.coupon.dto.CouponCreateRequest;
import dukku.semicolon.shared.coupon.dto.CouponResponse;
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

        return CouponResponse.from(coupon);
    }
}
