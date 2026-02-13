package dukku.user.boundedContext.user.exception;

import dukku.common.global.exception.BadRequestException;

public class EmailVerificationRequiredException extends BadRequestException {
    public EmailVerificationRequiredException() {
        super("회원가입을 위해 이메일 인증이 필요합니다.");
    }
}
