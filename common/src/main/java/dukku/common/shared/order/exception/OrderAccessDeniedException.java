package dukku.common.shared.order.exception;

import dukku.common.global.exception.ForbiddenException;

public class OrderAccessDeniedException extends ForbiddenException {
    public OrderAccessDeniedException() {
        super("본인의 주문만 반품 신청할 수 있습니다.");
    }
}
