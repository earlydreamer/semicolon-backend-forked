package dukku.semicolon.boundedContext.settlement.app;

import dukku.common.shared.deposit.event.DepositChargeFailedEvent;
import dukku.common.shared.deposit.event.DepositChargeSucceededEvent;
import dukku.common.shared.order.event.OrderItemConfirmedEvent;
import dukku.semicolon.boundedContext.settlement.entity.Settlement;
import dukku.semicolon.shared.settlement.dto.SettlementDetailResponse;
import dukku.semicolon.shared.settlement.dto.SettlementSearchCondition;
import dukku.semicolon.shared.settlement.dto.SettlementStatisticsCondition;
import dukku.semicolon.shared.settlement.dto.SettlementStatisticsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SettlementFacade {

    private final GetSettlementUseCase getSettlementUseCase;
    private final GetSettlementListUseCase getSettlementListUseCase;
    private final GetSettlementStatisticsUseCase getSettlementStatisticsUseCase;
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

    /**
     * 정산 수동 예치금 충전 요청 (PENDING → PROCESSING)
     */
//    public SettlementDetailResponse processSettlement(UUID settlementUuid) {
//        //TODO: deposit apl client 구현되면 생성
//
//    }
}
