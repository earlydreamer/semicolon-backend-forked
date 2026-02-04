package dukku.semicolon.boundedContext.settlement.app;

import dukku.semicolon.boundedContext.settlement.out.SettlementRepository;
import dukku.semicolon.shared.settlement.dto.SellerStatisticsResponse;
import dukku.semicolon.shared.settlement.dto.SellerStatisticsResponse.SellerSettlementSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 판매자 통계 조회 UseCase
 */
@Component
@RequiredArgsConstructor
public class GetSellerStatisticsUseCase {

    private final SettlementRepository settlementRepository;

    @Transactional(readOnly = true)
    public SellerStatisticsResponse execute(Pageable pageable) {
        List<SellerSettlementSummary> summaries = settlementRepository.getSellerStatistics(pageable);
        long totalSellers = settlementRepository.countDistinctSellers();
        long totalPages = (totalSellers + pageable.getPageSize() - 1) / pageable.getPageSize();

        return new SellerStatisticsResponse(summaries, (int) totalSellers, totalPages);
    }
}
