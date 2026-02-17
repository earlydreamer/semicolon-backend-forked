package dukku.common.shared.order.exception;

import dukku.common.global.exception.BadRequestException;

public class OrderRefundRequestInvalidException extends BadRequestException {
    public OrderRefundRequestInvalidException() {
        super("환불 이벤트 입력값이 유효하지 않습니다.");
    }
}
