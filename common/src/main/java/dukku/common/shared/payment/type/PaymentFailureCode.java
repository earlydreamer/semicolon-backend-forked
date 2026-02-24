package dukku.common.shared.payment.type;

/**
 * 결제 실패 코드를 표준화한 enum.
 */
public enum PaymentFailureCode {
    PAYMENT_STATUS_INVALID, // PENDING 상태 아님
    PAYMENT_EXPIRED, // 결제 가능 시간 만료
    AMOUNT_MISMATCH, // PG 금액 불일치
    DUPLICATE_PAYMENT_KEY, // PG 결제 키 중복
    PG_CONFIRM_FAILED, // PG 승인 실패 응답
    PG_CONFIRM_EXCEPTION, // PG 승인 호출 예외
    PG_CANCEL_FAILED, // PG 취소 실패 응답
    PG_CANCEL_EXCEPTION, // PG 취소 호출 예외
    DEPOSIT_DEDUCTION_FAILED, // 예치금 차감 실패
    REFUND_PG_CANCEL_FAILED, // 환불 PG 취소 실패 응답
    REFUND_PG_CANCEL_EXCEPTION, // 환불 PG 취소 호출 예외
    STATE_PERSIST_FAILED, // 상태/이력 저장 실패
    UNKNOWN // 미분류
}
