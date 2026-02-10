package dukku.common.shared.coupon.exception;

import dukku.common.global.exception.ConflictException;

public class CouponUpdateNotAllowedException extends ConflictException {
    public CouponUpdateNotAllowedException() {
        super("DRAFT 상태에서만 수정 가능합니다.");
    }
}
