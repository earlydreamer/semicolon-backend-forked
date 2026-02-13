package dukku.common.shared.user.exception;

import dukku.common.global.exception.ConflictException;

public class UserAlreadyWithdrawException extends ConflictException {

    public UserAlreadyWithdrawException() {
        super("\uC774\uBBF8 \uD0C8\uD1F4\uD55C \uC0AC\uC6A9\uC790\uC785\uB2C8\uB2E4.");
    }
}