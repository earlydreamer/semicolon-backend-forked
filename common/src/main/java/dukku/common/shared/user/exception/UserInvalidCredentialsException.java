package dukku.common.shared.user.exception;

import dukku.common.global.exception.UnauthorizedException;

public class UserInvalidCredentialsException extends UnauthorizedException {
    public UserInvalidCredentialsException() {
        super("\uC774\uBA54\uC77C \uB610\uB294 \uBE44\uBC00\uBC88\uD638\uAC00 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.");
    }
}