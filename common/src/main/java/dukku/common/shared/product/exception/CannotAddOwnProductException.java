package dukku.common.shared.product.exception;

import dukku.common.global.exception.BadRequestException;

public class CannotAddOwnProductException extends BadRequestException {
    public CannotAddOwnProductException() {
        super("자신의 상품은 장바구니에 담을 수 없습니다.");
    }
}
