package dukku.common.shared.coupon.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CouponUpdateRequest(
        @NotBlank(message = "쿠폰명은 필수입니다.")
        @Size(max = 100, message = "쿠폰명은 100자 이하여야 합니다.")
        String couponName,

        @Positive(message = "할인 금액은 0보다 커야 합니다.")
        int discountAmount,

        @PositiveOrZero(message = "최소 주문 금액은 0 이상이어야 합니다.")
        int minimumOrderAmount,

        @NotNull(message = "쿠폰 시작일은 필수입니다.")
        LocalDateTime validFrom
) {
}
