package dukku.common.shared.payment.type;

/**
 * 결제 실패가 발생한 단계.
 */
public enum PaymentFailureStage {
    VALIDATION, // PG 확인 전 검증
    PG_CONFIRM, // PG 승인 요청
    PG_CANCEL, // 보상/환불 중 PG 취소
    DEPOSIT_DEDUCTION, // 결제 성공 후 예치금 차감
    REFUND, // 환불 실행
    SYSTEM // 알 수 없는 시스템 오류
}
