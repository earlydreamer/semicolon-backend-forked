package dukku.common.shared.deposit.dto;

import dukku.common.shared.deposit.type.DepositChargeResultCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 정산을 위한 예치금 충전 응답 DTO (Internal API용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositChargeForSettlementResponse {

    private boolean success;
    private String code;
    private String message;
    private DepositChargeData data;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepositChargeData {
        private UUID depositUuid;
        private Long chargedAmount;
        private Long balanceAfter;
    }

    public static DepositChargeForSettlementResponse success(UUID depositUuid, Long chargedAmount, Long balanceAfter) {
        DepositChargeResultCode resultCode = DepositChargeResultCode.DEPOSIT_CHARGED;
        return DepositChargeForSettlementResponse.builder()
                .success(true)
                .code(resultCode.getCode())
                .message(resultCode.getMessage())
                .data(DepositChargeData.builder()
                        .depositUuid(depositUuid)
                        .chargedAmount(chargedAmount)
                        .balanceAfter(balanceAfter)
                        .build())
                .build();
    }

    public static DepositChargeForSettlementResponse failure(DepositChargeResultCode resultCode, String message) {
        return DepositChargeForSettlementResponse.builder()
                .success(false)
                .code(resultCode.getCode())
                .message(message)
                .build();
    }
}