package dukku.common.shared.payment.exception;

import dukku.common.global.exception.NotFoundException;

public class PaymentOrderItemNotFoundException extends NotFoundException {
    public PaymentOrderItemNotFoundException() {
        super("결제 내역에서 해당 주문 상품을 찾을 수 없습니다.");
    }
}
