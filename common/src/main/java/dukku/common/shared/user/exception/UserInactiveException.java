package dukku.common.shared.user.exception;

import dukku.common.global.exception.UnauthorizedException;

public class UserInactiveException extends UnauthorizedException {
    public UserInactiveException() {
        super("\uD0C8\uD1F4\uD55C \uC0AC\uC6A9\uC790\uB294 \uC694\uCCAD\uC744 \uCC98\uB9AC\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.");
    }
}