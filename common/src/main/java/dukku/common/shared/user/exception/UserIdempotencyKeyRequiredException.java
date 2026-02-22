package dukku.common.shared.user.exception;

import dukku.common.global.exception.BadRequestException;

public class UserIdempotencyKeyRequiredException extends BadRequestException {
    public UserIdempotencyKeyRequiredException() {
        super("Idempotency-Key 헤더는 필수입니다.");
    }
}
