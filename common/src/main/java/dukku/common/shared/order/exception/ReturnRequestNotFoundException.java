package dukku.common.shared.order.exception;

import dukku.common.global.exception.NotFoundException;

public class ReturnRequestNotFoundException extends NotFoundException {
    public ReturnRequestNotFoundException() {
        super("반품 요청 정보를 찾을 수 없습니다.");
    }
}
