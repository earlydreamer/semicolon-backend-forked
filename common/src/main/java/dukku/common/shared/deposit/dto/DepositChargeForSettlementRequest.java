package dukku.common.shared.deposit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 정산에 의한 예치금 충전 요청 DTO (Internal API용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositChargeForSettlementRequest {

    /**
     * 충전 금액
     */
    @NotNull(message = "충전 금액은 필수입니다.")
    @Positive(message = "충전 금액은 0보다 커야 합니다.")
    private Long amount;

    /**
     * 정산 UUID (멱등키로 활용)
     */
    @NotNull(message = "정산 UUID는 필수입니다.")
    private UUID settlementUuid;
}
