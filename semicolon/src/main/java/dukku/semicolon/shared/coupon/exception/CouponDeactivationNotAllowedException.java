package dukku.semicolon.shared.coupon.exception;

import dukku.common.global.exception.ConflictException;

public class CouponDeactivationNotAllowedException extends ConflictException {

    public CouponDeactivationNotAllowedException() {
        super("쿠폰은 ACTIVE 상태에서만 비활성화할 수 있습니다.");
    }
}