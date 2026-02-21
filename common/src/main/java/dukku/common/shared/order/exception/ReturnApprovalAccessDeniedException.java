package dukku.common.shared.order.exception;

import dukku.common.global.exception.ForbiddenException;

/**
 * 판매자 반품 승인 권한이 없는 경우 발생
 */
public class ReturnApprovalAccessDeniedException extends ForbiddenException {
    public ReturnApprovalAccessDeniedException() {
        super("본인의 판매 상품 반품 요청만 승인할 수 있습니다.");
    }
}
