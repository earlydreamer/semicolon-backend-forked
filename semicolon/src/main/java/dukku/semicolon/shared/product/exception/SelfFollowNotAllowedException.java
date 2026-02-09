package dukku.semicolon.shared.product.exception;

import dukku.common.global.exception.BadRequestException;

public class SelfFollowNotAllowedException extends BadRequestException {
    public SelfFollowNotAllowedException() {
        super("자기 자신을 팔로우할 수 없습니다.");
    }
}
