package dukku.user.boundedContext.user.exception;

import dukku.common.global.exception.BadRequestException;

public class EmailVerificationTokenInvalidException extends BadRequestException {
    public EmailVerificationTokenInvalidException() {
        super("이메일 인증 토큰이 만료되었거나 유효하지 않습니다.");
    }
}
