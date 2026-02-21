package dukku.common.shared.order.exception;

import dukku.common.global.exception.ForbiddenException;

/**
 * 주문 조회/수정 권한이 없는 경우 발생하는 예외.
 */
public class OrderAccessDeniedException extends ForbiddenException {
    public OrderAccessDeniedException() {
        super("주문에 접근할 권한이 없습니다.");
    }
}
