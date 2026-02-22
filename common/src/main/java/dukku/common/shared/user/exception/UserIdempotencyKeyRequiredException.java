package dukku.common.shared.user.exception;

import dukku.common.global.exception.BadRequestException;

public class UserIdempotencyKeyRequiredException extends BadRequestException {
    public UserIdempotencyKeyRequiredException() {
        super("요청을 처리할 수 없습니다. 다시 시도해 주세요.");
    }
}
