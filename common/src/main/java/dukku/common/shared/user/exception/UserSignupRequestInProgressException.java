package dukku.common.shared.user.exception;

import dukku.common.global.exception.ConflictException;

public class UserSignupRequestInProgressException extends ConflictException {
    public UserSignupRequestInProgressException() {
        super("동일한 회원가입 요청이 처리 중입니다. 잠시 후 다시 시도해주세요.");
    }
}
