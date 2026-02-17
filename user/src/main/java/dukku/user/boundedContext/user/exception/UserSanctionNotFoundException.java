package dukku.user.boundedContext.user.exception;

import dukku.common.global.exception.NotFoundException;

public class UserSanctionNotFoundException extends NotFoundException {
    public UserSanctionNotFoundException() {
        super("해당 회원 제재 이력을 찾을 수 없습니다.");
    }
}
