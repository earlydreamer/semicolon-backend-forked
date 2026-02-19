package dukku.common.shared.product.exception;

import dukku.common.global.exception.ConflictException;

public class CartProductAlreadyExistsException extends ConflictException {
    public CartProductAlreadyExistsException() {
        super("이미 장바구니에 담긴 상품입니다.");
    }
}
