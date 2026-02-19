package dukku.common.shared.user.exception;

import dukku.common.global.exception.ConflictException;

public class UserAlreadyWithdrawException extends ConflictException {

    public UserAlreadyWithdrawException() {
        super("이미 탈퇴한 사용자입니다.");
    }
}