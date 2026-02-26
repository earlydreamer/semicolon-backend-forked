package dukku.common.shared.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import dukku.common.shared.order.type.ReturnStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 판매자용 반품 요청 응답 DTO
 */
@Getter
@Builder
public class SellerReturnResponse {
    private UUID returnRequestUuid;   // 반품 요청 UUID
    private UUID orderUuid;           // 주문 UUID
    private ReturnStatus status;      // 반품 상태
    private String reason;            // 반품 사유
    private String rejectionReason;   // 거절 사유
    private String carrierName;       // 구매자 반송 택배사
    private String trackingNumber;    // 구매자 반송 운송장번호
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;  // 반품 신청 일시
    private List<ReturnItemSummary> returnItems;

    @Getter
    @Builder
    public static class ReturnItemSummary {
        private UUID orderItemUuid;  // 주문 아이템 UUID
        private UUID productUuid;
        private String productName;  // 상품명
        private String imageUrl;
        private int refundAmount;    // 환불 금액
    }
}
