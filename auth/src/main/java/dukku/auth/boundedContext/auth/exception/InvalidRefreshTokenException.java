package dukku.auth.boundedContext.auth.exception;

import dukku.common.global.exception.UnauthorizedException;

public class InvalidRefreshTokenException extends UnauthorizedException {
    public InvalidRefreshTokenException() {
        super("유효하지 않은 Refresh Token입니다.");
    }
}