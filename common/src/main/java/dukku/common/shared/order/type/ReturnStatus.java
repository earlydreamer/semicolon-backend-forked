package dukku.common.shared.order.type;

/**
 * 반품 요청 상태 코드
 */
public enum ReturnStatus {
    RETURN_REQUESTED,
    RETURN_SELLER_APPROVED,
    RETURN_SHIPPED,
    RETURN_APPROVED,
    RETURN_COMPLETED,
    RETURN_REJECTED_BEFORE_SHIPMENT,
    RETURN_REJECTED_AFTER_SHIPMENT,
    RETURN_REJECTED
}
