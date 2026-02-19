package dukku.common.shared.user.exception;

import dukku.common.global.exception.UnauthorizedException;

public class UserInvalidCredentialsException extends UnauthorizedException {
    public UserInvalidCredentialsException() {
        super("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}