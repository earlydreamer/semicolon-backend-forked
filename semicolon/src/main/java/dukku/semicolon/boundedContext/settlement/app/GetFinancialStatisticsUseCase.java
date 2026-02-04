package dukku.semicolon.boundedContext.settlement.app;

import dukku.semicolon.boundedContext.settlement.out.SettlementRepository;
import dukku.semicolon.shared.settlement.dto.FinancialStatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재무 통계 조회 UseCase
 * 플랫폼 수익, 정산 대기/처리/실패 금액 등
 */
@Component
@RequiredArgsConstructor
public class GetFinancialStatisticsUseCase {

    private final SettlementRepository settlementRepository;

    @Transactional(readOnly = true)
    public FinancialStatisticsResponse execute() {
        return settlementRepository.getFinancialStatistics();
    }
}
