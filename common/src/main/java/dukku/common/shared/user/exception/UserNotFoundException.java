package dukku.common.shared.user.exception;

import dukku.common.global.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException() {
        super("사용자를 찾을 수 없습니다.");
    }
}