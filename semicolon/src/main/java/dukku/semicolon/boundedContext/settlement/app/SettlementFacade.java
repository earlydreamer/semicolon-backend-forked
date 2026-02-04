package dukku.semicolon.boundedContext.settlement.app;

import dukku.semicolon.boundedContext.settlement.entity.Settlement;
import dukku.semicolon.shared.settlement.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SettlementFacade {

    private final GetSettlementUseCase getSettlementUseCase;
    private final GetSettlementListUseCase getSettlementListUseCase;
    private final GetSettlementStatisticsUseCase getSettlementStatisticsUseCase;
    private final GetBatchStatisticsUseCase getBatchStatisticsUseCase;
    private final GetFinancialStatisticsUseCase getFinancialStatisticsUseCase;
    private final GetTrendStatisticsUseCase getTrendStatisticsUseCase;
    private final GetSellerStatisticsUseCase getSellerStatisticsUseCase;
    private final CreateSettlementUseCase createSettlementUseCase;
    private final SettlementSupport settlementSupport;
    private final RequestDepositChargeUseCase requestDepositChargeUseCase;
    private final RetrySettlementUseCase retrySettlementUseCase;
    private final ManualCompleteSettlementUseCase manualCompleteSettlementUseCase;
    private final ManualFailSettlementUseCase manualFailSettlementUseCase;
    private final ManualProcessSettlementUseCase manualProcessSettlementUseCase;

    @Transactional(readOnly = true)
    public SettlementDetailResponse getSettlement(UUID settlementUuid) {
        Settlement settlement = getSettlementUseCase.execute(settlementUuid);
        return SettlementDetailResponse.from(settlement);
    }

    @Transactional(readOnly = true)
    public Page<SettlementDetailResponse> getSettlements(SettlementSearchCondition condition, Pageable pageable) {
        Page<Settlement> settlements = getSettlementListUseCase.execute(condition, pageable);
        return settlements.map(SettlementDetailResponse::from);
    }

    @Transactional(readOnly = true)
    public SettlementStatisticsResponse getStatistics(SettlementStatisticsCondition condition) {
        return getSettlementStatisticsUseCase.execute(condition);
    }

    // ===== 리포트용 통계 API =====

    /**
     * 배치 Job 통계 조회
     */
    @Transactional(readOnly = true)
    public BatchJobStatisticsResponse getBatchJobStatistics(LocalDate startDate, LocalDate endDate) {
        return getBatchStatisticsUseCase.getJobStatistics(startDate, endDate);
    }

    /**
     * 배치 Step 통계 조회
     */
    @Transactional(readOnly = true)
    public BatchStepStatisticsResponse getBatchStepStatistics(LocalDate startDate, LocalDate endDate) {
        return getBatchStatisticsUseCase.getStepStatistics(startDate, endDate);
    }

    /**
     * 재무 통계 조회
     */
    @Transactional(readOnly = true)
    public FinancialStatisticsResponse getFinancialStatistics() {
        return getFinancialStatisticsUseCase.execute();
    }

    /**
     * 트렌드 통계 조회
     */
    @Transactional(readOnly = true)
    public TrendStatisticsResponse getTrendStatistics(LocalDate startDate, LocalDate endDate) {
        return getTrendStatisticsUseCase.execute(startDate, endDate);
    }

    /**
     * 판매자별 정산 통계 조회
     */
    @Transactional(readOnly = true)
    public SellerStatisticsResponse getSellerStatistics(Pageable pageable) {
        return getSellerStatisticsUseCase.execute(pageable);
    }

    // ===== 정산 처리 API =====

    /**
     * 실패한 정산 재처리 (FAILED → PENDING)
     */
    public SettlementDetailResponse retrySettlement(UUID settlementUuid) {
        Settlement settlement = retrySettlementUseCase.execute(settlementUuid);
        return SettlementDetailResponse.from(settlement);
    }

    /**
     * 정산 수동 완료 처리 (PROCESSING → SUCCESS)
     */
    public SettlementDetailResponse completeSettlement(UUID settlementUuid) {
        Settlement settlement = manualCompleteSettlementUseCase.execute(settlementUuid);
        return SettlementDetailResponse.from(settlement);
    }

    /**
     * 정산 수동 실패 처리 (PROCESSING → FAILED)
     */
    public SettlementDetailResponse failSettlement(UUID settlementUuid) {
        Settlement settlement = manualFailSettlementUseCase.execute(settlementUuid);
        return SettlementDetailResponse.from(settlement);
    }
}
