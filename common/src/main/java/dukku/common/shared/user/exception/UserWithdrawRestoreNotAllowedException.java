package dukku.common.shared.user.exception;

import dukku.common.global.exception.ConflictException;

public class UserWithdrawRestoreNotAllowedException extends ConflictException {
    private UserWithdrawRestoreNotAllowedException(String message) {
        super(message);
    }

    public static UserWithdrawRestoreNotAllowedException emailAlreadyInUse() {
        return new UserWithdrawRestoreNotAllowedException("\uBCF5\uAD6C\uD560 \uC774\uBA54\uC77C\uC774 \uC774\uBBF8 \uC0AC\uC6A9 \uC911\uC785\uB2C8\uB2E4.");
    }

    public static UserWithdrawRestoreNotAllowedException userIsNotWithdrawn() {
        return new UserWithdrawRestoreNotAllowedException("\uD0C8\uD1F4 \uC0C1\uD0DC\uC778 \uC0AC\uC6A9\uC790\uB9CC \uBCF5\uAD6C\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.");
    }

    public static UserWithdrawRestoreNotAllowedException restoreWindowExpired() {
        return new UserWithdrawRestoreNotAllowedException("\uBCF5\uAD6C \uAC00\uB2A5 \uAE30\uAC04\uC774 \uB9CC\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4.");
    }

    public static UserWithdrawRestoreNotAllowedException userIsNotRestorable() {
        return new UserWithdrawRestoreNotAllowedException("\uD604\uC7AC \uC0C1\uD0DC\uC5D0\uC11C\uB294 \uD0C8\uD1F4 \uBCF5\uAD6C\uB97C \uC9C4\uD589\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.");
    }

    public static UserWithdrawRestoreNotAllowedException missingWithdrawalBackup() {
        return new UserWithdrawRestoreNotAllowedException("\uD0C8\uD1F4 \uBCF5\uAD6C\uC5D0 \uD544\uC694\uD55C \uBC31\uC5C5 \uC815\uBCF4\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.");
    }
}