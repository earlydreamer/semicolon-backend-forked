package dukku.common.shared.settlement.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 이상거래 탐지 유형
 */
@Getter
@RequiredArgsConstructor
public enum AnomalyType {
    // 구현된 규칙
    AMOUNT_MISMATCH("PG 정산금액 vs 플랫폼 금액 불일치", Severity.CRITICAL),
    FEE_CALCULATION_ERROR("수수료 계산 오류", Severity.HIGH),
    REFUNDED_ORDER_SETTLEMENT("환불 후 정산 건 탐지", Severity.CRITICAL),
    DUPLICATE_SETTLEMENT("동일 주문 중복 정산", Severity.CRITICAL),
    HIGH_REFUND_RATE_SELLER("셀러별 높은 환불률", Severity.HIGH),
    NEW_SELLER_HIGH_RISK("신규 셀러 고위험 거래", Severity.HIGH),

    // TODO: API 확장 필요
    REFUND_DEDUCTION_MISSING("환불 차감 누락", Severity.CRITICAL),
    EXCESSIVE_PARTIAL_REFUND("과다 부분환불", Severity.HIGH),
    EARLY_SETTLEMENT_BEFORE_DELIVERY("배송 전 조기 정산", Severity.HIGH);

    private final String description;
    private final Severity severity;


    public boolean isCritical() {
        return severity == Severity.CRITICAL;
    }

    public enum Severity {
        CRITICAL, HIGH, MEDIUM, LOW
    }
}
