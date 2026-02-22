package dukku.common.shared.user.exception;

import dukku.common.global.exception.ConflictException;

public class UserSignupIdempotencyConflictException extends ConflictException {
    public UserSignupIdempotencyConflictException() {
        super("동일한 멱등성 키로 다른 회원가입 요청을 보낼 수 없습니다.");
    }
}
