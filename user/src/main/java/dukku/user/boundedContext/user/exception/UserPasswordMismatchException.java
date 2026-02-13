package dukku.user.boundedContext.user.exception;

import dukku.common.global.exception.UnauthorizedException;

public class UserPasswordMismatchException extends UnauthorizedException {
    public UserPasswordMismatchException() {
        super("현재 비밀번호가 일치하지 않습니다.");
    }
}
