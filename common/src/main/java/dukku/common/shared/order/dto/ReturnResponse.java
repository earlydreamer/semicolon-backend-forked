package dukku.common.shared.order.dto;

import dukku.common.shared.order.type.ReturnStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReturnResponse {
    private UUID returnRequestUuid;
    private UUID orderUuid;
    private ReturnStatus status;
    private String reason;
    private String carrierName;
    private String carrierCode;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private List<ReturnItemResponse> returnItems;

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
