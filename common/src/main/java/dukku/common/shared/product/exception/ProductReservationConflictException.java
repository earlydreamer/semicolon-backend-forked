package dukku.common.shared.product.exception;

import dukku.common.global.exception.ConflictException;

public class ProductReservationConflictException extends ConflictException {
    public ProductReservationConflictException() {
        super("이미 거래 중인 상품입니다.");
    }

    public ProductReservationConflictException(String details) {
        super(details);
    }
}
