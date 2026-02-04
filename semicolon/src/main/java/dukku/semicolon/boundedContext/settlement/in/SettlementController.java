package dukku.semicolon.boundedContext.settlement.in;

import dukku.semicolon.boundedContext.settlement.app.SettlementFacade;
import dukku.semicolon.shared.settlement.docs.SettlementApiDocs;
import dukku.semicolon.shared.settlement.dto.SettlementDetailResponse;
import dukku.semicolon.shared.settlement.dto.SettlementSearchRequest;
import dukku.semicolon.shared.settlement.dto.SettlementStatisticsRequest;
import dukku.semicolon.shared.settlement.dto.SettlementStatisticsResponse;
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
     * GET /admin/settlements
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
     * GET /admin/settlements/{settlementUuid}
     */
    @GetMapping("/{settlementUuid}")
    @SettlementApiDocs.GetSettlement
    public SettlementDetailResponse getSettlement(@PathVariable UUID settlementUuid) {
        return settlementFacade.getSettlement(settlementUuid);
    }

    /**
     * 정산 통계 조회
     * GET /admin/settlements/statistics
     */
    @GetMapping("/statistics")
    @SettlementApiDocs.GetSettlementStatistics
    public SettlementStatisticsResponse getStatistics(
            @Valid @ModelAttribute SettlementStatisticsRequest request
    ) {
        return settlementFacade.getStatistics(request.toCondition());
    }

    /**
     * 실패한 정산 재처리
     * POST /admin/settlements/{settlementUuid}/retry
     */
    @PostMapping("/{settlementUuid}/retry")
    @SettlementApiDocs.RetrySettlement
    public SettlementDetailResponse retrySettlement(@PathVariable UUID settlementUuid) {
        return settlementFacade.retrySettlement(settlementUuid);
    }

    /**
     * 정산 수동 완료 처리
     * POST /admin/settlements/{settlementUuid}/complete
     */
    @PostMapping("/{settlementUuid}/complete")
    @SettlementApiDocs.CompleteSettlement
    public SettlementDetailResponse completeSettlement(@PathVariable UUID settlementUuid) {
        return settlementFacade.completeSettlement(settlementUuid);
    }

    /**
     * 정산 수동 실패 처리
     * POST /admin/settlements/{settlementUuid}/fail
     */
    @PostMapping("/{settlementUuid}/fail")
    @SettlementApiDocs.FailSettlement
    public SettlementDetailResponse failSettlement(@PathVariable UUID settlementUuid) {
        return settlementFacade.failSettlement(settlementUuid);
    }

    /**
     * 정산 수동 예치금 충전 요청
     * POST /admin/settlements/{settlementUuid}/process
     */
//    @PostMapping("/{settlementUuid}/process")
//    @SettlementApiDocs.ProcessSettlement
//    public SettlementDetailResponse processSettlement(@PathVariable UUID settlementUuid) {
//        return settlementFacade.processSettlement(settlementUuid);
//    }
}
