package dukku.common.shared.user.exception;

import dukku.common.global.exception.BadRequestException;

public class UserEmailVerificationTokenInvalidException extends BadRequestException {
    public UserEmailVerificationTokenInvalidException() {
        super("이메일 인증 토큰이 만료되었거나 유효하지 않습니다.");
    }
}