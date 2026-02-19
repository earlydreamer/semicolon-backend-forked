package dukku.common.shared.user.exception;

import dukku.common.global.exception.BadRequestException;
import dukku.common.shared.user.type.SocialProvider;

public class UserInvalidSocialProviderException extends BadRequestException {
    private UserInvalidSocialProviderException(String details) {
        super(details);
    }

    public static UserInvalidSocialProviderException unsupported(SocialProvider provider) {
        return new UserInvalidSocialProviderException("Unsupported social provider: " + provider);
    }
}
