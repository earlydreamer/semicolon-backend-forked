package dukku.common.shared.user.exception;

import dukku.common.global.exception.NotFoundException;

public class UserAddressNotFoundException extends NotFoundException {
    public UserAddressNotFoundException() {
        super("주소를 찾지 못했습니다.");
    }
}
