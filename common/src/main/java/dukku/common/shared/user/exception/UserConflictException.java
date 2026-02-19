package dukku.common.shared.user.exception;

import dukku.common.global.exception.ConflictException;

public class UserConflictException extends ConflictException {
    public UserConflictException() {
        super("이미 가입된 이메일입니다.");
    }
}