package dukku.user.boundedContext.user.exception;

import dukku.common.global.exception.ConflictException;

public class AlreadyWithdrawUserException extends ConflictException {

    public AlreadyWithdrawUserException() {
        super("이미 탈퇴한 사용자입니다.");
    }
}
