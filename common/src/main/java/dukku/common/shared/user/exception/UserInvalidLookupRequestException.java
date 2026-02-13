package dukku.common.shared.user.exception;

import dukku.common.global.exception.BadRequestException;

public class UserInvalidLookupRequestException extends BadRequestException {
    private UserInvalidLookupRequestException(String message) {
        super(message);
    }

    public static UserInvalidLookupRequestException roleOrEmailOnly() {
        return new UserInvalidLookupRequestException("role \uB610\uB294 email \uC911 \uD558\uB098\uB9CC \uC804\uB2EC\uD574\uC57C \uD569\uB2C8\uB2E4.");
    }

    public static UserInvalidLookupRequestException emailBlank() {
        return new UserInvalidLookupRequestException("email \uAC12\uC740 \uBE44\uC5B4 \uC788\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.");
    }
}