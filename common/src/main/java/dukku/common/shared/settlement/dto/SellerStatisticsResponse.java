package dukku.common.shared.settlement.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 판매자 통계 응답 DTO
 */
public record SellerStatisticsResponse(
        List<SellerSettlementSummary> sellerSummaries,
        int totalSellerCount,
        long totalPages
) {
    /**
     * 판매자별 정산 요약
     */
    public record SellerSettlementSummary(
            UUID sellerUuid,
            long totalSettlementCount,
            long successCount,
            long failedCount,
            long pendingCount,
            BigDecimal successRate,
            long totalSettledAmount,
            long totalFeeAmount
    ) {
        public static SellerSettlementSummary of(
                UUID sellerUuid,
                long totalSettlementCount,
                long successCount,
                long failedCount,
                long pendingCount,
                long totalSettledAmount,
                long totalFeeAmount
        ) {
            BigDecimal successRate = totalSettlementCount > 0
                    ? BigDecimal.valueOf(successCount * 100.0 / totalSettlementCount)
                    .setScale(2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            return new SellerSettlementSummary(
                    sellerUuid,
                    totalSettlementCount,
                    successCount,
                    failedCount,
                    pendingCount,
                    successRate,
                    totalSettledAmount,
                    totalFeeAmount
            );
        }
    }
}
