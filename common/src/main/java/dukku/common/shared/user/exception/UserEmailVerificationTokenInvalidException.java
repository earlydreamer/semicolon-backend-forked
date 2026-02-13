package dukku.common.shared.user.exception;

import dukku.common.global.exception.BadRequestException;

public class UserEmailVerificationTokenInvalidException extends BadRequestException {
    public UserEmailVerificationTokenInvalidException() {
        super("\uC774\uBA54\uC77C \uC778\uC99D \uD1A0\uD070\uC774 \uB9CC\uB8CC\uB418\uC5C8\uAC70\uB098 \uC720\uD6A8\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.");
    }
}