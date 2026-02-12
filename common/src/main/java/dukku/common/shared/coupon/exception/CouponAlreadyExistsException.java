package dukku.common.shared.coupon.exception;

import dukku.common.global.exception.ConflictException;

public class CouponAlreadyExistsException extends ConflictException {
    public CouponAlreadyExistsException() {
        super("이미 쿠폰을 발급받은 유저입니다.");
    }
}
