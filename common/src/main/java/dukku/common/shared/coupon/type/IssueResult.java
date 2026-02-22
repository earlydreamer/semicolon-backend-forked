package dukku.common.shared.coupon.type;

public enum IssueResult {
    SUCCESS,      // 발급 성공
    SOLD_OUT,     // 소진
    DUPLICATE,    // 중복 발급
    INACTIVE,     // 비활성 쿠폰
    ERROR         // 오류
}
