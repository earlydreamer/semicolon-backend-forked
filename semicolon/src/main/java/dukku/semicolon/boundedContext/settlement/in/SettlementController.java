package dukku.semicolon.boundedContext.settlement.in;

import dukku.semicolon.boundedContext.settlement.app.SettlementFacade;
import dukku.semicolon.shared.settlement.docs.SettlementApiDocs;
import dukku.semicolon.shared.settlement.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/settlements")
@RequiredArgsConstructor
@SettlementApiDocs.SettlementTag
public class SettlementController {

    private final SettlementFacade settlementFacade;

    /**
     * 정산 목록 조회
     */
    @GetMapping
    @SettlementApiDocs.GetSettlements
    public Page<SettlementDetailResponse> getSettlements(
            @Valid @ModelAttribute SettlementSearchRequest request,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return settlementFacade.getSettlements(request.toCondition(), pageable);
    }

    /**
     * 정산 단건 조회
     */
    @GetMapping("/{settlementUuid}")
    @SettlementApiDocs.GetSettlement
    public SettlementDetailResponse getSettlement(@PathVariable UUID settlementUuid) {
        return settlementFacade.getSettlement(settlementUuid);
    }

    /**
     * 정산 통계 조회 (기존)
     */
    @GetMapping("/statistics")
    @SettlementApiDocs.GetSettlementStatistics
    public SettlementStatisticsResponse getStatistics(
            @Valid @ModelAttribute SettlementStatisticsRequest request
    ) {
        return settlementFacade.getStatistics(request.toCondition());
    }

    // ===== 리포트용 통계 API =====

    /**
     * 배치 Job 통계 조회
     */
    @GetMapping("/statistics/batch/jobs")
    @SettlementApiDocs.GetBatchJobStatistics
    public BatchJobStatisticsResponse getBatchJobStatistics(
            @Valid @ModelAttribute SettlementReportRequest request
    ) {
        return settlementFacade.getBatchJobStatistics(request.startDate(), request.endDate());
    }

    /**
     * 배치 Step 통계 조회
     */
    @GetMapping("/statistics/batch/steps")
    @SettlementApiDocs.GetBatchStepStatistics
    public BatchStepStatisticsResponse getBatchStepStatistics(
            @Valid @ModelAttribute SettlementReportRequest request
    ) {
        return settlementFacade.getBatchStepStatistics(request.startDate(), request.endDate());
    }

    /**
     * 재무 통계 조회
     */
    @GetMapping("/statistics/financial")
    @SettlementApiDocs.GetFinancialStatistics
    public FinancialStatisticsResponse getFinancialStatistics() {
        return settlementFacade.getFinancialStatistics();
    }

    /**
     * 트렌드 통계 조회
     */
    @GetMapping("/statistics/trend")
    @SettlementApiDocs.GetTrendStatistics
    public TrendStatisticsResponse getTrendStatistics(
            @Valid @ModelAttribute SettlementReportRequest request
    ) {
        return settlementFacade.getTrendStatistics(request.startDate(), request.endDate());
    }

    /**
     * 판매자별 정산 통계 조회
     */
    @GetMapping("/statistics/sellers")
    @SettlementApiDocs.GetSellerStatistics
    public SellerStatisticsResponse getSellerStatistics(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return settlementFacade.getSellerStatistics(pageable);
    }

    // ===== 정산 처리 API =====

    /**
     * 실패한 정산 재처리
     */
    @PostMapping("/{settlementUuid}/retry")
    @SettlementApiDocs.RetrySettlement
    public SettlementDetailResponse retrySettlement(@PathVariable UUID settlementUuid) {
        return settlementFacade.retrySettlement(settlementUuid);
    }

    /**
     * 정산 수동 완료 처리
     */
    @PostMapping("/{settlementUuid}/complete")
    @SettlementApiDocs.CompleteSettlement
    public SettlementDetailResponse completeSettlement(@PathVariable UUID settlementUuid) {
        return settlementFacade.completeSettlement(settlementUuid);
    }

    /**
     * 정산 수동 실패 처리
     */
    @PostMapping("/{settlementUuid}/fail")
    @SettlementApiDocs.FailSettlement
    public SettlementDetailResponse failSettlement(@PathVariable UUID settlementUuid) {
        return settlementFacade.failSettlement(settlementUuid);
    }
}
