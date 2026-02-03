package dukku.semicolon.shared.deposit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 정산에 의한 예치금 충전 응답 DTO (Internal API용)
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
        return DepositChargeForSettlementResponse.builder()
                .success(true)
                .code("DEPOSIT_CHARGED")
                .message("예치금이 충전되었습니다.")
                .data(DepositChargeData.builder()
                        .depositUuid(depositUuid)
                        .chargedAmount(chargedAmount)
                        .balanceAfter(balanceAfter)
                        .build())
                .build();
    }

    public static DepositChargeForSettlementResponse failure(String code, String message) {
        return DepositChargeForSettlementResponse.builder()
                .success(false)
                .code(code)
                .message(message)
                .build();
    }
}
