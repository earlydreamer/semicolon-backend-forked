package dukku.semicolon.boundedContext.settlement.app;

import dukku.semicolon.boundedContext.settlement.out.SettlementReportRepository;
import dukku.common.shared.settlement.dto.SettlementFinancialStatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재무 통계 조회 UseCase
 */
@Component
@RequiredArgsConstructor
public class GetFinancialStatisticsUseCase {

    private final SettlementReportRepository reportRepository;

    @Transactional(readOnly = true)
    public SettlementFinancialStatisticsResponse execute() {
        return reportRepository.getFinancialStatistics();
    }
}
