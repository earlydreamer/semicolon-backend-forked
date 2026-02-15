package dukku.common.shared.user.exception;

import dukku.common.global.exception.UnauthorizedException;

public class UserInactiveException extends UnauthorizedException {
    public UserInactiveException() {
        super("탈퇴한 사용자는 요청을 처리할 수 없습니다.");
    }
}