package dukku.common.shared.user.exception;

import dukku.common.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UserSignupHashingFailedException extends BaseException {
    public UserSignupHashingFailedException() {
        super(
                "USER_SIGNUP_HASHING_FAILED",
                "회원가입 요청 처리 중 내부 오류가 발생했습니다.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                null
        );
    }
}
