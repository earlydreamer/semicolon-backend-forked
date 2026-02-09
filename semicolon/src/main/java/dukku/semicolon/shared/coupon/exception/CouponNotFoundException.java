package dukku.semicolon.shared.coupon.exception;

import dukku.common.global.exception.NotFoundException;

public class CouponNotFoundException extends NotFoundException {
    public CouponNotFoundException() {
        super("존재하지 않는 쿠폰입니다.");
    }
}
