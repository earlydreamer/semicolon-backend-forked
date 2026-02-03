package dukku.semicolon.shared.product.exception;

import dukku.common.global.exception.NotFoundException;

public class CommentNotFoundException extends NotFoundException {
    public CommentNotFoundException() {
        super("존재하지 않는 댓글입니다.");
    }
}
