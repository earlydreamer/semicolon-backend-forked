package dukku.common.shared.order.exception;

import dukku.common.global.exception.ForbiddenException;

/**
 * 사용자 권한으로 허용되지 않은 주문 상품 상태 변경을 시도한 경우 발생하는 예외.
 */
public class OrderItemActionNotAllowedException extends ForbiddenException {
    public OrderItemActionNotAllowedException() {
        super("사용자가 변경할 수 없는 상태입니다.");
    }
}
