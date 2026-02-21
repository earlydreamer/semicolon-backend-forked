package dukku.common.shared.order.exception;

import dukku.common.global.exception.NotFoundException;

public class OrderItemNotFoundException extends NotFoundException {
    public OrderItemNotFoundException() {
        super("주문 상품을 찾을 수 없습니다.");
    }
}
