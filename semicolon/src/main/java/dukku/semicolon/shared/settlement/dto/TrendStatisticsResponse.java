package dukku.semicolon.shared.settlement.dto;

import java.util.List;

/**
 * 트렌드 통계 응답 DTO
 */
public record TrendStatisticsResponse(
        // 일별 정산 금액 추이
        List<DailyTrend> dailyTrends,

        // 월별 거래량 추이
        List<MonthlyTrend> monthlyTrends,

        // 처리 시간 분석
        ProcessingTimeStats processingTimeStats
) {
    /**
     * 일별 트렌드
     */
    public record DailyTrend(
            String date,
            long settlementCount,
            long settlementAmount,
            long feeAmount,
            long totalAmount
    ) {}

    /**
     * 월별 트렌드
     */
    public record MonthlyTrend(
            int year,
            int month,
            long settlementCount,
            long settlementAmount,
            long feeAmount,
            long totalAmount
    ) {}

    /**
     * 처리 시간 통계
     */
    public record ProcessingTimeStats(
            // 평균 처리 시간 (예약일 → 완료일, 시간 단위)
            double avgReservationToCompletionHours,
            // 평균 처리 시간 (생성일 → 완료일, 시간 단위)
            double avgCreationToCompletionHours,
            // 최소 처리 시간
            double minProcessingHours,
            // 최대 처리 시간
            double maxProcessingHours
    ) {}
}
