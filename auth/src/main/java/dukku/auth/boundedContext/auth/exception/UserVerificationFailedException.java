package dukku.auth.boundedContext.auth.exception;

import dukku.common.global.exception.UnauthorizedException;

public class UserVerificationFailedException extends UnauthorizedException {
    public UserVerificationFailedException() {
        super("사용자 인증에 실패했습니다.");
    }
}