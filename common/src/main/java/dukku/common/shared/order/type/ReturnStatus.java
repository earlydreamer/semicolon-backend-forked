package dukku.common.shared.order.type;

public enum ReturnStatus {
    RETURN_REQUESTED, // 반품 신청됨
    RETURN_SHIPPED, // 반품 발송됨 (운송장 등록 완료)
    RETURN_APPROVED, // 판매자 반품 수락 (환불 트리거)
    RETURN_COMPLETED, // 반품 완료 (부분환불 완료)
    RETURN_REJECTED // 반품 거절
}
