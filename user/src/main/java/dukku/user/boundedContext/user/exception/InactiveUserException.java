package dukku.user.boundedContext.user.exception;

import dukku.common.global.exception.UnauthorizedException;

public class InactiveUserException extends UnauthorizedException {
    public InactiveUserException() {
        super("탈퇴한 사용자는 요청을 처리할 수 없습니다.");
    }
}
