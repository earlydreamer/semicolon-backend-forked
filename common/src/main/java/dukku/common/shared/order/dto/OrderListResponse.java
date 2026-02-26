package dukku.common.shared.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import dukku.common.shared.order.type.OrderItemStatus;
import dukku.common.shared.order.type.OrderStatus;
import dukku.common.shared.order.type.ReturnStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class OrderListResponse {
    private UUID orderUuid;           // 주문 ID
    private UUID returnRequestUuid;
    private ReturnStatus returnStatus;
    private String returnCarrierName;
    private String returnTrackingNumber;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderDate;  // 주문 날짜
    private OrderStatus status;       // 주문 전체 상태 (예: 결제완료)
    private int totalAmount;          // 총 결제 금액
    private List<SimpleOrderItemResponse> items;

    @Getter
    @Builder
    public static class SimpleOrderItemResponse {
        private UUID orderItemUuid; // 주문 상품 UUID
        private UUID productUuid; // 상품 UUID
        private String productName; // 상품명
        private int productPrice; // 상품 가격
        private String imageUrl; // 상품 이미지 URL
        private OrderItemStatus itemStatus; // 개별 상품 상태 (예: 배송중, 구매확정)
        private String carrierName;    // 택배사명
        private String trackingNumber; // 운송장 번호
    }
}
