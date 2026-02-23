package dukku.common.shared.user.exception;

import dukku.common.global.exception.ConflictException;

public class UserSignupIdempotencyConflictException extends ConflictException {
    public UserSignupIdempotencyConflictException() {
        super("회원가입 요청 정보가 일치하지 않습니다. 다시 시도해 주세요.");
    }
}
