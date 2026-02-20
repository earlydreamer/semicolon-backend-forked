package dukku.common.shared.coupon.exception;

import dukku.common.global.exception.NotFoundException;

/**
 * 존재하지 않는 쿠폰을 조회할 때 발생하는 예외
 *
 * <p>
 * 쿠폰 UUID에 해당하는 쿠폰이 DB에 없거나 Internal API에서 404가 반환될 때 사용
 */
public class CouponNotFoundException extends NotFoundException {
    public CouponNotFoundException() {
        super("존재하지 않는 쿠폰입니다.");
    }
}
