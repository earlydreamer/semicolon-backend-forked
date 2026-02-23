package dukku.common.shared.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import dukku.common.shared.order.type.OrderItemStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 판매자가 본인 판매 주문아이템을 조회할 때 사용하는 응답 DTO
 */
@Getter
@Builder
public class SellerOrderItemResponse {
    private UUID orderItemUuid;   // 주문아이템 UUID
    private UUID orderUuid;       // 주문 UUID
    private String productName;   // 상품명
    private int productPrice;     // 상품 가격
    private String imageUrl;      // 상품 이미지
    private OrderItemStatus itemStatus; // 주문아이템 상태
    private String carrierName;   // 택배사명
    private String carrierCode;   // 택배사 코드
    private String trackingNumber; // 운송장 번호
    private String buyerAddress;  // 배송지 주소
    private String recipient;     // 수령인
    private String contactNumber; // 연락처
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderedAt; // 주문 시각
}
