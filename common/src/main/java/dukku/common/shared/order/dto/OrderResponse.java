package dukku.common.shared.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import dukku.common.shared.order.type.OrderItemStatus;
import dukku.common.shared.order.type.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private UUID orderUuid; // 주문 UUID
    private UUID userUuid; // 구매자 UUID
    private int totalAmount; // 주문 총액
    private int refundedAmount; // 누적 환불 금액
    private OrderStatus orderStatus; // 주문 상태
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderedAt; // 주문 시각
    private String recipient; // 수령인
    private String contactNumber; // 수령인 연락처
    private String address; // 배송지 주소
    private List<OrderItemResponse> items; // 주문 상품 목록

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private UUID orderItemUuid; // 주문 상품 UUID
        private UUID productUuid;   // 상품 UUID
        private UUID sellerUuid;    // 판매자 UUID
        private String productName; // 상품명
        private int productPrice;   // 상품 가격
        private String imageUrl;    // 상품 이미지 URL
        private OrderItemStatus itemStatus; // 주문 상품 상태
        private String carrierName;    // 택배사 명
        private String trackingNumber; // 운송장 번호
    }
}
