package dukku.common.shared.user.exception;

import dukku.common.global.exception.BadRequestException;

public class UserInvalidLookupRequestException extends BadRequestException {
    private UserInvalidLookupRequestException(String message) {
        super(message);
    }

    public static UserInvalidLookupRequestException roleOrEmailOnly() {
        return new UserInvalidLookupRequestException("role 또는 email 중 하나만 전달해야 합니다.");
    }

    public static UserInvalidLookupRequestException emailBlank() {
        return new UserInvalidLookupRequestException("email 값은 비어 있을 수 없습니다.");
    }
}