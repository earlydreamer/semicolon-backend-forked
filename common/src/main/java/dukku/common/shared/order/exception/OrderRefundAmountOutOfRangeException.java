package dukku.common.shared.order.exception;

import dukku.common.global.exception.BadRequestException;

/**
 * 주문 환불 금액이 유효 범위를 벗어난 경우 발생하는 예외입니다
 */
public class OrderRefundAmountOutOfRangeException extends BadRequestException {
    public OrderRefundAmountOutOfRangeException() {
        super("주문 환불 금액이 유효하지 않습니다");
    }
}
