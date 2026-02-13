package dukku.common.shared.user.exception;

import dukku.common.global.exception.BadRequestException;

public class UserEmailVerificationRequiredException extends BadRequestException {
    public UserEmailVerificationRequiredException() {
        super("\uD68C\uC6D0\uAC00\uC785\uC744 \uC704\uD574 \uC774\uBA54\uC77C \uC778\uC99D\uC774 \uD544\uC694\uD569\uB2C8\uB2E4.");
    }
}