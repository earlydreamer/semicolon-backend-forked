package dukku.semicolon.shared.settlement.dto;

import java.util.List;

/**
 * 배치 Step 실행 통계 응답 DTO
 */
public record SettlementBatchStepStatisticsResponse(
        List<StepPerformance> stepPerformances
) {
    /**
     * Step별 성능 통계
     */
    public record StepPerformance(
            String stepName,
            double avgDurationSeconds,
            double avgReadCount,
            double avgWriteCount,
            double avgSkipCount,
            long totalExecutions
    ) {}
}
