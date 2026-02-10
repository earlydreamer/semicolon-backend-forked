package dukku.common.shared.product.exception;

import dukku.common.global.exception.UnauthorizedException;

public class ReviewUnauthorizedException extends UnauthorizedException {
    public ReviewUnauthorizedException() {
        super("리뷰 작성자가 아닙니다.");
    }
}
