package dukku.common.shared.coupon.dto;

import dukku.common.shared.coupon.type.CouponStatus;

/**
 * 쿠폰 내부 조회 응답 DTO
 *
 * <p>
 * 결제 서비스 등 내부 모듈에서 쿠폰 정보를 조회할 때 사용한다.
 *
 * @param discountAmount     쿠폰 할인 금액 (원)
 * @param minimumOrderAmount 최소 주문 금액 (이 금액 미만이면 쿠폰 사용 불가)
 * @param status             쿠폰 상태 ({@link CouponStatus})
 */
public record CouponInternalResponse(
        int discountAmount,
        int minimumOrderAmount,
        CouponStatus status
) {
}
