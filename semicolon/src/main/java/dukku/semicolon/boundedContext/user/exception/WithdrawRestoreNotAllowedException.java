package dukku.semicolon.boundedContext.user.exception;

import dukku.common.global.exception.ConflictException;

public class WithdrawRestoreNotAllowedException extends ConflictException {
    public WithdrawRestoreNotAllowedException(String message) {
        super(message);
    }
}
