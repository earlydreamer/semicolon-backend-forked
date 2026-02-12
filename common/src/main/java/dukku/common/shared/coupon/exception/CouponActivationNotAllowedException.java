package dukku.common.shared.coupon.exception;

import dukku.common.global.exception.ConflictException;

public class CouponActivationNotAllowedException extends ConflictException {

    public CouponActivationNotAllowedException() {
        super("쿠폰은 DRAFT 상태에서만 활성화할 수 있습니다.");
    }
}
