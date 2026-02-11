package dukku.common.shared.settlement.dto;


import dukku.common.shared.settlement.type.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 정산 상세 응답 DTO (외부 정보 포함)
 * - Settlement 엔티티 정보 + 외부 BC 정보 조합
 * - 관리자 화면 표시용 (판매자 닉네임, 상품명, 계좌 정보 포함)
 */
public record SettlementDetailResponse(
        UUID settlementUuid,
        SettlementStatus status,
        UUID sellerUuid,
        String sellerNickname,      // 외부 User BC에서 조회
        String productName,          // 외부 Product BC에서 조회
        Long totalAmount,
        BigDecimal fee,
        Long feeAmount,
        Long settlementAmount,
        LocalDateTime settlementReservationDate,
        UUID orderUuid,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
