package dukku.common.shared.product.exception;

import dukku.common.global.exception.BadRequestException;

public class CannotAddSoldOutProductException extends BadRequestException {
    public CannotAddSoldOutProductException() {
        super("판매 완료된 상품은 장바구니에 담을 수 없습니다.");
    }
}
