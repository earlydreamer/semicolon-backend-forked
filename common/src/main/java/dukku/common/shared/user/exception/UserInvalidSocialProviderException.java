package dukku.common.shared.user.exception;

import dukku.common.global.exception.BadRequestException;
import dukku.common.shared.user.type.SocialProvider;

public class UserInvalidSocialProviderException extends BadRequestException {
    private UserInvalidSocialProviderException(String details) {
        super(details);
    }

    public static UserInvalidSocialProviderException unsupported(SocialProvider provider) {
        return new UserInvalidSocialProviderException("지원하지 않는 소셜 로그인 제공자입니다: " + provider);
    }
}
