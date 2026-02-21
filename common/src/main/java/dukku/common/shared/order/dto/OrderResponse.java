package dukku.common.shared.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import dukku.common.shared.order.type.OrderItemStatus;
import dukku.common.shared.order.type.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class OrderResponse {
    // 1. 주문 기본 정보
    private UUID orderUuid;
    private UUID userUuid;

    // 2. 결제 및 상태 정보
    private int totalAmount;
    private int refundedAmount;
    private OrderStatus orderStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderedAt;

    // 3. 배송지 정보
    private String recipient;
    private String contactNumber;
    private String address;

    private List<OrderItemResponse> items;

    @Getter
    @Builder
    public static class OrderItemResponse {
        private UUID orderItemUuid; // 주문 상품 UUID
        private Integer productId;  // 상품 PK
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