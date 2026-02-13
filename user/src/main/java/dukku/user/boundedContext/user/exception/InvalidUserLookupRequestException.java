package dukku.user.boundedContext.user.exception;

import dukku.common.global.exception.BadRequestException;

public class InvalidUserLookupRequestException extends BadRequestException {
    private InvalidUserLookupRequestException(String message) {
        super(message);
    }

    public static InvalidUserLookupRequestException roleOrEmailOnly() {
        return new InvalidUserLookupRequestException("role 또는 email 중 하나만 전달해야 합니다.");
    }

    public static InvalidUserLookupRequestException emailBlank() {
        return new InvalidUserLookupRequestException("email 값은 비어 있을 수 없습니다.");
    }
}
