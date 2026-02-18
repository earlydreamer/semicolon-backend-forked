package dukku.coupon.boundedContext.coupon.app.command;

import dukku.common.shared.coupon.exception.CouponUserNotFoundException;
import dukku.coupon.boundedContext.coupon.entity.CouponUser;
import dukku.coupon.boundedContext.coupon.out.CouponUserRepository;
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

    // 결제 실패 보상: 사용 처리된 쿠폰을 AVAILABLE로 복구한다
    public void rollbackForPayment(UUID userUuid, UUID couponUuid) {
        CouponUser couponUser = couponUserRepository.findByUserUuidAndCoupon_Uuid(userUuid, couponUuid)
                .orElseThrow(CouponUserNotFoundException::new);

        couponUser.rollbackUseForPayment();
    }
}
