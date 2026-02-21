package dukku.common.shared.order.exception;

import dukku.common.global.exception.ConflictException;

public class ReturnItemSelectionRequiredException extends ConflictException {
    public ReturnItemSelectionRequiredException() {
        super("반품할 상품을 하나 이상 선택해주세요.");
    }
}
