package dukku.common.shared.user.exception;

import dukku.common.global.exception.UnauthorizedException;

public class UserPasswordMismatchException extends UnauthorizedException {
    public UserPasswordMismatchException() {
        super("\uD604\uC7AC \uBE44\uBC00\uBC88\uD638\uAC00 \uC77C\uCE58\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.");
    }
}