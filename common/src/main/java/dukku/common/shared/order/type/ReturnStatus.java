package dukku.common.shared.order.type;

/**
 * 반품 요청 상태 코드
 */
public enum ReturnStatus {
    RETURN_REQUESTED,                   // 반품 요청
    RETURN_SELLER_APPROVED,             // 판매자 반품 승인
    RETURN_SHIPPED,                     // 반품 배송 중
    RETURN_APPROVED,                    // 반품 승인
    RETURN_COMPLETED,                   // 반품 완료
    RETURN_REJECTED_BEFORE_SHIPMENT,    // 배송 전 반품 거절
    RETURN_REJECTED_AFTER_SHIPMENT,     // 배송 후 반품 거절
    RETURN_REJECTED                     // 반품 거절
}
