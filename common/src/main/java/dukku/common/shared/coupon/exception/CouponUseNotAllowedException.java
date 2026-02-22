package dukku.common.shared.coupon.exception;

import dukku.common.global.exception.ConflictException;

/**
 * 사용 불가 상태의 쿠폰을 사용하려 할 때 발생하는 예외
 *
 * <p>
 * 이미 사용된 쿠폰, 만료된 쿠폰, 비활성(INACTIVE) 쿠폰 등에 적용
 */
public class CouponUseNotAllowedException extends ConflictException {

    public CouponUseNotAllowedException() {
        super("사용할 수 없는 쿠폰입니다.");
    }
}
