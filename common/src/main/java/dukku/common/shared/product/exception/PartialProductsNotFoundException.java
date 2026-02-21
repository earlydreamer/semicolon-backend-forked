package dukku.common.shared.product.exception;

import dukku.common.global.exception.NotFoundException;

/**
 * 일부(또는 여러) 상품을 찾을 수 없음을 나타내는 도메인 예외입니다.
 */
public class PartialProductsNotFoundException extends NotFoundException {
    public PartialProductsNotFoundException() {
        super("일부 상품을 찾을 수 없습니다.");
    }
}
