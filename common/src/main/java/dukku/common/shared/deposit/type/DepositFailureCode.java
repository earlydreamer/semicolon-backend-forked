package dukku.common.shared.deposit.type;

/**
 * 예치금 실패 코드를 표준화한 enum.
 */
public enum DepositFailureCode {
    BALANCE_SHORTAGE, // 잔액 부족
    SYSTEM_ERROR, // 시스템 오류
    PERSISTENCE_ERROR, // 저장 실패
    UNKNOWN // 미분류
}
