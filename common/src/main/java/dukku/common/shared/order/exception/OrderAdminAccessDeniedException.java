package dukku.common.shared.order.exception;

import dukku.common.global.exception.ForbiddenException;

/**
 * 관리자 전용 주문 조회 기능을 일반 사용자가 호출한 경우 발생하는 예외.
 */
public class OrderAdminAccessDeniedException extends ForbiddenException {
    public OrderAdminAccessDeniedException() {
        super("관리자만 조회할 수 있습니다.");
    }
}
