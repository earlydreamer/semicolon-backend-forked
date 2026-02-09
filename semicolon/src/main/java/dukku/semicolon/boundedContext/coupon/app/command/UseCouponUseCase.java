package dukku.semicolon.boundedContext.coupon.app.command;

import dukku.semicolon.boundedContext.coupon.entity.CouponUser;
import dukku.semicolon.boundedContext.coupon.out.CouponUserRepository;
import dukku.semicolon.shared.coupon.exception.CouponUserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class UseCouponUseCase {
    private final CouponUserRepository couponUserRepository;

    public void execute(UUID userUuid, UUID couponUuid) {
        CouponUser couponUser = couponUserRepository.findByUserUuidAndCoupon_Uuid(userUuid, couponUuid)
                .orElseThrow(CouponUserNotFoundException::new);

        couponUser.use();
    }
}
