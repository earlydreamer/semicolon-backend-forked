package dukku.common.shared.product.exception;

import dukku.common.global.exception.NotFoundException;

public class SellerNotFoundException extends NotFoundException {
    public SellerNotFoundException() {
        super("판매자를 찾을 수 없습니다.");
    }
}
