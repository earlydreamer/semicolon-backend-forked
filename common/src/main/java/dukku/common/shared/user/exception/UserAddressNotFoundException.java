package dukku.common.shared.user.exception;

import dukku.common.global.exception.NotFoundException;

public class UserAddressNotFoundException extends NotFoundException {
    public UserAddressNotFoundException() {
        super("Address not found.");
    }
}
