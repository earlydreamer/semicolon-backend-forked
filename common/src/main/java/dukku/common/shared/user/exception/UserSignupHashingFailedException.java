package dukku.common.shared.user.exception;

import dukku.common.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UserSignupHashingFailedException extends BaseException {
    public UserSignupHashingFailedException() {
        super(
                "USER_SIGNUP_HASHING_FAILED",
                "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                null
        );
    }
}
