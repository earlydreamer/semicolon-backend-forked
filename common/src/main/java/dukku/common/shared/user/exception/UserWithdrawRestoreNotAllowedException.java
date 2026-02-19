package dukku.common.shared.user.exception;

import dukku.common.global.exception.ConflictException;

public class UserWithdrawRestoreNotAllowedException extends ConflictException {
    private UserWithdrawRestoreNotAllowedException(String message) {
        super(message);
    }

    public static UserWithdrawRestoreNotAllowedException emailAlreadyInUse() {
        return new UserWithdrawRestoreNotAllowedException("복구할 이메일이 이미 사용 중입니다.");
    }

    public static UserWithdrawRestoreNotAllowedException userIsNotWithdrawn() {
        return new UserWithdrawRestoreNotAllowedException("탈퇴 상태인 사용자만 복구할 수 있습니다.");
    }

    public static UserWithdrawRestoreNotAllowedException restoreWindowExpired() {
        return new UserWithdrawRestoreNotAllowedException("복구 가능 기간이 만료되었습니다.");
    }

    public static UserWithdrawRestoreNotAllowedException userIsNotRestorable() {
        return new UserWithdrawRestoreNotAllowedException("현재 상태에서는 탈퇴 복구를 진행할 수 없습니다.");
    }

    public static UserWithdrawRestoreNotAllowedException missingWithdrawalBackup() {
        return new UserWithdrawRestoreNotAllowedException("탈퇴 복구에 필요한 백업 정보가 없습니다.");
    }
}