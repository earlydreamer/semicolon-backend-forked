package dukku.semicolon.shared.coupon.exception;

import dukku.common.global.exception.ConflictException;

public class CouponIssueNotAllowedException extends ConflictException {

    public CouponIssueNotAllowedException() {
        super("활성화된 쿠폰만 발급할 수 있습니다.");
    }
}
