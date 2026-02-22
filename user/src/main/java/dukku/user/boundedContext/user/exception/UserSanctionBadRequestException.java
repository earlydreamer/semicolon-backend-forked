package dukku.user.boundedContext.user.exception;

import dukku.common.global.exception.BadRequestException;

public class UserSanctionBadRequestException extends BadRequestException {
    private UserSanctionBadRequestException(String message) {
        super(message);
    }

    public static UserSanctionBadRequestException invalidPeriod() {
        return new UserSanctionBadRequestException("제재 시작일과 종료일이 올바르지 않습니다.");
    }

    public static UserSanctionBadRequestException warningMustNotHaveEndAt() {
        return new UserSanctionBadRequestException("경고는 종료일을 입력할 수 없습니다.");
    }

    public static UserSanctionBadRequestException suspensionRequiresEndAt() {
        return new UserSanctionBadRequestException("일시정지는 종료일이 필요합니다.");
    }

    public static UserSanctionBadRequestException permanentBanMustNotHaveEndAt() {
        return new UserSanctionBadRequestException("영구정지는 종료일 없이 등록해야 합니다.");
    }
}
