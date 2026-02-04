package dukku.semicolon.boundedContext.settlement.out;

import com.querydsl.core.Tuple;
import dukku.semicolon.boundedContext.settlement.entity.Settlement;
import dukku.semicolon.shared.settlement.dto.*;
import dukku.semicolon.shared.settlement.dto.TrendStatisticsResponse.DailyTrend;
import dukku.semicolon.shared.settlement.dto.TrendStatisticsResponse.MonthlyTrend;
import dukku.semicolon.shared.settlement.dto.TrendStatisticsResponse.ProcessingTimeStats;
import dukku.semicolon.shared.settlement.dto.SellerStatisticsResponse.SellerSettlementSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface SettlementRepositoryCustom {

    Page<Settlement> search(SettlementSearchCondition condition, Pageable pageable);

    /**
     * 전체 통계 조회
     */
    Tuple getTotalStatistics();

    /**
     * 상태별 통계 조회 (groupBy)
     */
    List<Tuple> getStatisticsByStatus();

    /**
     * 동적 조건에 따른 정산 건수 조회
     */
    long countByCondition(SettlementStatisticsCondition condition);

    /**
     * 동적 조건에 따른 정산 금액 합계 조회
     */
    long sumSettlementAmountByCondition(SettlementStatisticsCondition condition);

    // ===== 리포트용 메서드 (DTO 직접 반환) =====

    /**
     * 재무 통계 조회
     */
    FinancialStatisticsResponse getFinancialStatistics();

    /**
     * 판매자별 통계 조회 (페이징)
     */
    List<SellerSettlementSummary> getSellerStatistics(Pageable pageable);

    /**
     * 전체 판매자 수 조회
     */
    long countDistinctSellers();

    /**
     * 일별 정산 트렌드 조회
     */
    List<DailyTrend> getDailyTrend(LocalDate startDate, LocalDate endDate);

    /**
     * 월별 정산 트렌드 조회
     */
    List<MonthlyTrend> getMonthlyTrend(LocalDate startDate, LocalDate endDate);

    /**
     * 처리 시간 통계 조회 (SUCCESS 상태 기준)
     */
    ProcessingTimeStats getProcessingTimeStats();
}
