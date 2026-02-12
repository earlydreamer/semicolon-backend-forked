package dukku.common.shared.coupon.type;

public enum CouponStatus {
    DRAFT,      // 생성 후 발급 전
    ACTIVE,     // 발급 가능
    INACTIVE,   // 발급 중단/비활성
    EXPIRED     // 만료
}
