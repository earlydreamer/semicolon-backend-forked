package dukku.common.shared.order.exception;

import dukku.common.global.exception.ForbiddenException;

public class ReturnRequestAccessDeniedException extends ForbiddenException {
    public ReturnRequestAccessDeniedException() {
        super("본인의 반품 요청에만 운송장을 등록할 수 있습니다.");
    }
}
