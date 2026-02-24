package dukku.common.shared.coupon.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CouponIssueToUserRequest(
        @NotNull(message = "userUuid는 필수입니다.")
        UUID userUuid
) {
}
