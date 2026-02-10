package dukku.common.shared.product.exception;

import dukku.common.global.exception.ForbiddenException;

public class CommentUnauthorizedException extends ForbiddenException {
    public CommentUnauthorizedException() {
        super("댓글에 대한 권한이 없습니다.");
    }
}
