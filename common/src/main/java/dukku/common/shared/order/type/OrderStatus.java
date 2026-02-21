package dukku.common.shared.order.type;

public enum OrderStatus {
    PENDING,             // 결제 대기
    PAID,                // 결제 완료
    PAYMENT_FAILED,      // 결제 실패
    CANCELED,            // 주문 취소
    PARTIAL_REFUNDED;    // 부분 환불
}
