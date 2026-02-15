package dukku.common.shared.user.exception;

import dukku.common.global.exception.BadRequestException;

public class UserEmailVerificationRequiredException extends BadRequestException {
    public UserEmailVerificationRequiredException() {
        super("회원가입을 위해 이메일 인증이 필요합니다.");
    }
}