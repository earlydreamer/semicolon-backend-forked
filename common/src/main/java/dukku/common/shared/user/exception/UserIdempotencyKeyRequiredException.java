package dukku.common.shared.user.exception;

import dukku.common.global.exception.BadRequestException;

public class UserIdempotencyKeyRequiredException extends BadRequestException {
    public UserIdempotencyKeyRequiredException() {
        super("멱등성 키 헤더는 필수입니다.");
    }
}
