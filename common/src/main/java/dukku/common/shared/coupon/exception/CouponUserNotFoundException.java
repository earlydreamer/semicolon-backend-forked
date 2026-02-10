package dukku.common.shared.coupon.exception;

import dukku.common.global.exception.NotFoundException;

public class CouponUserNotFoundException extends NotFoundException {
    public CouponUserNotFoundException() {
        super("쿠폰 발급 기록이 없습니다.");
    }
}
