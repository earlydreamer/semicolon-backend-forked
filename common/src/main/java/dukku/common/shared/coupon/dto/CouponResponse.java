package dukku.common.shared.coupon.dto;

import dukku.common.shared.coupon.type.CouponStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CouponResponse(
        UUID uuid,
        String couponName,
        int discountAmount,
        int minimumOrderAmount,
        LocalDateTime validFrom,
        LocalDateTime createdAt,
        CouponStatus status,
        int totalQuantity,
        int issuedQuantity
) {
}
