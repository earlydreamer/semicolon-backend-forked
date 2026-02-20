package dukku.common.shared.product.exception;

import dukku.common.global.exception.NotFoundException;

public class CartItemNotFoundException extends NotFoundException {
    public CartItemNotFoundException() {
        super("장바구니 항목을 찾을 수 없습니다.");
    }
}
