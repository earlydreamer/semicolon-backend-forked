package dukku.common.shared.coupon.exception;

import dukku.common.global.exception.ConflictException;

public class CouponUseNotAllowedException extends ConflictException {

    public CouponUseNotAllowedException() {
        super("사용할 수 없는 쿠폰입니다.");
    }
}
