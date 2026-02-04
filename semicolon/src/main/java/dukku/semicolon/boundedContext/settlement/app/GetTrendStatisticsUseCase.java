package dukku.semicolon.boundedContext.settlement.app;

import dukku.semicolon.boundedContext.settlement.out.SettlementRepository;
import dukku.semicolon.shared.settlement.dto.TrendStatisticsResponse;
import dukku.semicolon.shared.settlement.dto.TrendStatisticsResponse.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 트렌드 통계 조회 UseCase
 * 일별/월별 정산 추이, 처리 시간 분석
 */
@Component
@RequiredArgsConstructor
public class GetTrendStatisticsUseCase {

    private final SettlementRepository settlementRepository;

    @Transactional(readOnly = true)
    public TrendStatisticsResponse execute(LocalDate startDate, LocalDate endDate) {
        List<DailyTrend> dailyTrends = settlementRepository.getDailyTrend(startDate, endDate);
        List<MonthlyTrend> monthlyTrends = settlementRepository.getMonthlyTrend(startDate, endDate);
        ProcessingTimeStats processingTimeStats = settlementRepository.getProcessingTimeStats();

        return new TrendStatisticsResponse(dailyTrends, monthlyTrends, processingTimeStats);
    }
}
