package dukku.semicolon.shared.coupon.dto;

import dukku.semicolon.boundedContext.coupon.entity.Coupon;
import dukku.semicolon.boundedContext.coupon.entity.type.CouponStatus;

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
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getUuid(),
                coupon.getCouponName(),
                coupon.getDiscountAmount(),
                coupon.getMinimumOrderAmount(),
                coupon.getValidFrom(),
                coupon.getCreatedAt(),
                coupon.getStatus(),
                coupon.getTotalQuantity(),
                coupon.getIssuedQuantity()
        );
    }
}
