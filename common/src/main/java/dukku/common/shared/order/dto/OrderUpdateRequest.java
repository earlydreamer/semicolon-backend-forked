package dukku.common.shared.order.dto;

import dukku.common.shared.order.type.OrderStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 수정 요청 DTO 모음
 */
public class OrderUpdateRequest {

    /**
     * 배송지 정보 수정 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    public static class ShippingInfo {
        private String address; // 배송지 주소
        private String recipient; // 수령인 이름
        private String contactNumber; // 수령인 연락처
    }

    /**
     * 환불 금액 수정 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    public static class Refund {
        private int refundedAmount; // 누적 환불 금액
    }

    /**
     * 주문 상태 수정 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    public static class Status {
        private OrderStatus status; // 변경할 주문 상태
    }
}
