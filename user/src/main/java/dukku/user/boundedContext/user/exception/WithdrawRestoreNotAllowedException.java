package dukku.user.boundedContext.user.exception;

import dukku.common.global.exception.ConflictException;

public class WithdrawRestoreNotAllowedException extends ConflictException {
    private WithdrawRestoreNotAllowedException(String message) {
        super(message);
    }

    public static WithdrawRestoreNotAllowedException emailAlreadyInUse() {
        return new WithdrawRestoreNotAllowedException("복구할 이메일이 이미 사용 중입니다.");
    }

    public static WithdrawRestoreNotAllowedException userIsNotWithdrawn() {
        return new WithdrawRestoreNotAllowedException("탈퇴 상태인 사용자만 복구할 수 있습니다.");
    }

    public static WithdrawRestoreNotAllowedException restoreWindowExpired() {
        return new WithdrawRestoreNotAllowedException("복구 가능 기간이 만료되었습니다.");
    }

    public static WithdrawRestoreNotAllowedException userIsNotRestorable() {
        return new WithdrawRestoreNotAllowedException("현재 상태에서는 탈퇴 복구를 진행할 수 없습니다.");
    }

    public static WithdrawRestoreNotAllowedException missingWithdrawalBackup() {
        return new WithdrawRestoreNotAllowedException("탈퇴 복구에 필요한 백업 정보가 없습니다.");
    }
}
