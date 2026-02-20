package dukku.common.shared.user.exception;

import dukku.common.global.exception.BadRequestException;

public class UserAddressLimitExceededException extends BadRequestException {
    public UserAddressLimitExceededException(int maxAddressCount) {
        super("Address limit exceeded. Maximum allowed addresses: " + maxAddressCount);
    }
}
