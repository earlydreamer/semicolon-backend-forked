package dukku.coupon.boundedContext.coupon.entity;

import dukku.common.shared.coupon.exception.CouponUseNotAllowedException;
import dukku.common.shared.coupon.type.CouponStatus;
import dukku.common.shared.coupon.type.CouponUserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponUserPaymentSagaTest {

    @Test
    @DisplayName("이미 사용된 쿠폰에 use()를 호출하면 CouponUseNotAllowedException이 발생한다")
    void useThrowsWhenAlreadyUsed() {
        CouponUser couponUser = CouponUser.create(UUID.randomUUID(), activeCoupon());
        couponUser.use();

        assertThatThrownBy(couponUser::use)
                .isInstanceOf(CouponUseNotAllowedException.class);
    }

    @Test
    @DisplayName("payment.failed 보상 시 rollbackUseForPayment가 USED를 AVAILABLE로 되돌린다")
    void rollbackUseForPaymentRestoresAvailable() {
        CouponUser couponUser = CouponUser.create(UUID.randomUUID(), activeCoupon());
        couponUser.use();

        couponUser.rollbackUseForPayment();

        assertThat(couponUser.getStatus()).isEqualTo(CouponUserStatus.AVAILABLE);
        assertThat(couponUser.getUsedAt()).isNull();
    }

    @Test
    @DisplayName("AVAILABLE 상태에서 rollbackUseForPayment는 무시된다")
    void rollbackSkippedWhenAvailable() {
        CouponUser couponUser = CouponUser.create(UUID.randomUUID(), activeCoupon());

        couponUser.rollbackUseForPayment();

        assertThat(couponUser.getStatus()).isEqualTo(CouponUserStatus.AVAILABLE);
    }

    private Coupon activeCoupon() {
        return Coupon.builder()
                .uuid(UUID.randomUUID())
                .couponName("coupon")
                .discountAmount(1000)
                .minimumOrderAmount(1000)
                .validFrom(LocalDateTime.now().minusDays(1))
                .status(CouponStatus.ACTIVE)
                .totalQuantity(100)
                .issuedQuantity(1)
                .build();
    }
}
