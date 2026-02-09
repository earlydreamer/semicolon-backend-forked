package dukku.semicolon.shared.product.exception;

import dukku.common.global.exception.NotFoundException;

public class ReviewNotFoundException extends NotFoundException {
    public ReviewNotFoundException() {
        super("해당 리뷰를 찾을 수 없습니다.");
    }
}
