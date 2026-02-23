package dukku.common.shared.product.exception;

import dukku.common.global.exception.NotFoundException;

public class ProductUserNotFoundException extends NotFoundException {
    public ProductUserNotFoundException() {
        super("판매자 사용자 정보를 찾을 수 없습니다.");
    }
}
