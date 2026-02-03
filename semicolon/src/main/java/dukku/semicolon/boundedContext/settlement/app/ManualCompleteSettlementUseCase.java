package dukku.semicolon.boundedContext.settlement.app;

import dukku.semicolon.boundedContext.settlement.entity.Settlement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 정산 수동 완료 UseCase
 * - Settlement 상태: PROCESSING → SUCCESS
 * - 관리자가 정산을 수동으로 완료 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ManualCompleteSettlementUseCase {

    private final SettlementSupport settlementSupport;

    @Transactional
    public Settlement execute(UUID settlementUuid) {
        log.info("[정산 수동 완료] settlementUuid={}", settlementUuid);

        Settlement settlement = settlementSupport.findByUuid(settlementUuid);
        settlement.complete();
        settlementSupport.save(settlement);

        log.info("[정산 수동 완료 처리됨] settlementUuid={}, completedAt={}",
                settlementUuid, settlement.getCompletedAt());

        return settlement;
    }
}
