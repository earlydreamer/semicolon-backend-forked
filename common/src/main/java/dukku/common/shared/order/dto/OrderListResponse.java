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
public class OrderListResponse {
    private UUID orderUuid;           // 주문 ID
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderDate;  // 주문 날짜
    private OrderStatus status;       // 주문 전체 상태 (예: 결제완료)
    private int totalAmount;          // 총 결제 금액

    private List<SimpleOrderItemResponse> items;

    @Getter
    @Builder
    public static class SimpleOrderItemResponse {
        private UUID productUuid;
        private String productName;
        private int productPrice;
        private String imageUrl;
        private OrderItemStatus itemStatus; // 개별 상품 상태 (예: 배송중, 구매확정)
    }
}