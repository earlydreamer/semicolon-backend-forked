package dukku.common.shared.deposit.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DepositChargeResultCode {
    DEPOSIT_CHARGED("DEPOSIT_CHARGED", "예치금이 충전되었습니다."),
    INVALID_AMOUNT("INVALID_AMOUNT", "충전 금액은 0보다 커야 합니다."),
    CHARGE_FAILED("CHARGE_FAILED", "예치금 충전 중 오류가 발생했습니다.");

    private final String code;
    private final String message;
}