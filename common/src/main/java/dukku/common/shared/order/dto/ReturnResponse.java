package dukku.common.shared.order.dto;

import dukku.common.shared.order.type.ReturnStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 반품 요청 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReturnResponse {
    private UUID returnRequestUuid; // 반품 요청 UUID
    private UUID orderUuid; // 주문 UUID
    private ReturnStatus status; // 반품 상태
    private String reason; // 반품 사유
    private String rejectionReason; // 반품 거절 사유
    private String carrierName; // 반품 택배사 명
    private String carrierCode; // 반품 택배사 코드
    private String trackingNumber; // 반품 운송장 번호
    private LocalDateTime createdAt; // 반품 요청 생성 시각
    private List<ReturnItemResponse> returnItems; // 반품 아이템 목록

    /**
     * 반품 아이템 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class ReturnItemResponse {
        private UUID returnItemUuid; // 반품 아이템 UUID
        private UUID orderItemUuid; // 주문 아이템 UUID
        private int refundAmount; // 환불 금액
    }
}
