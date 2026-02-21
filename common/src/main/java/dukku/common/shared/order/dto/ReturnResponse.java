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
    private UUID returnRequestUuid;
    private UUID orderUuid;
    private ReturnStatus status;
    private String reason;
    private String rejectionReason;
    private String carrierName;
    private String carrierCode;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private List<ReturnItemResponse> returnItems;

    /**
     * 반품 아이템 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class ReturnItemResponse {
        private UUID returnItemUuid;
        private UUID orderItemUuid;
        private int refundAmount;
    }
}
