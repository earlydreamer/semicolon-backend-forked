package dukku.common.shared.settlement.dto;

import java.math.BigDecimal;

/**
 * 재무 통계 응답 DTO
 */
public record SettlementFinancialStatisticsResponse(
        // 플랫폼 수익 (성공한 정산의 수수료 총합)
        long platformRevenue,

        // 정산 대기 금액 (PENDING 상태의 정산 금액 총합)
        long pendingSettlementAmount,

        // 처리 중 금액 (PROCESSING 상태의 정산 금액 총합)
        long processingSettlementAmount,

        // 실패한 정산 금액 (FAILED 상태의 정산 금액 총합)
        long failedSettlementAmount,

        // 총 거래액 (SUCCESS 상태의 총액 합계)
        long totalTransactionAmount,

        // 총 정산 완료 금액
        long totalSettledAmount,

        // 평균 수수료율
        BigDecimal averageFeeRate
) {
}
