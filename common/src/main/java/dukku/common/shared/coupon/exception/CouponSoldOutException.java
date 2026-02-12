package dukku.common.shared.coupon.exception;

import dukku.common.global.exception.ConflictException;

public class CouponSoldOutException extends ConflictException {
    public CouponSoldOutException() {
        super("쿠폰 수량이 모두 소진되었습니다.");
    }
}