package dukku.common.shared.order.exception;

import dukku.common.global.exception.BadRequestException;

/**
 * 주문 환불 금액이 허용 범위를 벗어날 때 발생하는 예외.
 */
public class OrderRefundAmountOutOfRangeException extends BadRequestException {
    public OrderRefundAmountOutOfRangeException() {
        super("주문 환불 금액이 유효하지 않습니다.");
    }
}
