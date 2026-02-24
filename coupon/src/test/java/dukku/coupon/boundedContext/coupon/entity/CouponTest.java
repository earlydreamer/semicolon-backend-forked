package dukku.coupon.boundedContext.coupon.entity;

import dukku.common.shared.coupon.dto.CouponCreateRequest;
import dukku.common.shared.coupon.type.CouponStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CouponTest {

    @Test
    @DisplayName("createCoupon 호출 시 UUID가 자동 생성된다")
    void createCouponGeneratesUuid() {
        CouponCreateRequest request = new CouponCreateRequest(
                "신규 쿠폰",
                5000,
                10000,
                LocalDateTime.now().plusDays(1),
                100
        );

        Coupon coupon = Coupon.createCoupon(request);

        assertThat(coupon.getUuid()).isNotNull();
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.DRAFT);
    }
}
