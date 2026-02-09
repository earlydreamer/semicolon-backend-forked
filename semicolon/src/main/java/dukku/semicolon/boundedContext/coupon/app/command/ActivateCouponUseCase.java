package dukku.semicolon.boundedContext.coupon.app.command;

import dukku.semicolon.boundedContext.coupon.entity.Coupon;
import dukku.semicolon.boundedContext.coupon.out.CouponRepository;
import dukku.semicolon.shared.coupon.exception.CouponNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class ActivateCouponUseCase {
    private final CouponRepository couponRepository;

    public void execute(UUID couponUuid) {
        Coupon coupon = couponRepository.findByUuid(couponUuid)
                .orElseThrow(CouponNotFoundException::new);

        coupon.activate();
    }
}

