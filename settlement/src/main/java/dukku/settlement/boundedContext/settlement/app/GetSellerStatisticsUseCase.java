package dukku.settlement.boundedContext.settlement.app;

import dukku.settlement.boundedContext.settlement.out.SettlementReportRepository;
import dukku.common.shared.settlement.dto.SellerStatisticsResponse;
import dukku.common.shared.settlement.dto.SellerStatisticsResponse.SellerSettlementSummary;
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

    private final SettlementReportRepository reportRepository;

    @Transactional(readOnly = true)
    public SellerStatisticsResponse execute(Pageable pageable) {
        List<SellerSettlementSummary> summaries = reportRepository.getSellerStatistics(
                pageable.getPageSize(),
                (int) pageable.getOffset()
        );
        long totalSellers = reportRepository.countDistinctSellers();
        long totalPages = (totalSellers + pageable.getPageSize() - 1) / pageable.getPageSize();

        return new SellerStatisticsResponse(summaries, (int) totalSellers, totalPages);
    }
}
