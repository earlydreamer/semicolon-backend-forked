package dukku.settlement.boundedContext.settlement.batch.processor;

import dukku.settlement.boundedContext.settlement.app.service.AnomalyDetectionService;
import dukku.settlement.boundedContext.settlement.entity.Settlement;
import dukku.common.shared.settlement.exception.SettlementValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Step 2: 이상거래 탐지 + 금액 검증 Processor
 * - 이상거래 탐지 (CRITICAL 시 FAILED 처리)
 * - Settlement 금액 유효성 검증
 * - PENDING → PROCESSING 상태 전이
 * <p>
 * [책임]
 * - 이상거래 탐지 후 금액 검증 수행
 * - 실제 예치금 충전은 Step 3에서 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ValidateSettlementProcessor implements ItemProcessor<Settlement, Settlement> {

    private final AnomalyDetectionService anomalyDetectionService;

    @Override
    public Settlement process(Settlement settlement) throws Exception {
        log.debug("[Step 2 Processor] 검증 시작. settlementUuid={}, status={}",
                settlement.getUuid(), settlement.getSettlementStatus());

        // 1. 이상거래 탐지
        if (anomalyDetectionService.detect(settlement)) {
            // CRITICAL 이상거래 탐지 → 이미 FAILED 상태
            // Writer에서 저장 후, 다음 Step(depositCharge)에서 PROCESSING만 조회하므로 제외됨
            log.info("[Step 2 Processor] CRITICAL 이상거래로 FAILED 처리. settlementUuid={}",
                    settlement.getUuid());
            return settlement;
        }

        try {
            // 2. PENDING → PROCESSING 상태 전이 (내부에서 금액 검증 수행)
            // Settlement.startProcessing() 내부에서 validateForProcessing() 호출
            settlement.startProcessing();

            log.debug("[Step 2 Processor] 금액 검증 성공. settlementUuid={}, newStatus={}",
                    settlement.getUuid(), settlement.getSettlementStatus());

            return settlement;

        } catch (SettlementValidationException e) {
            // 데이터 유효성 오류 → Skip 처리
            log.error("[Step 2 Processor-Skip] 금액 검증 실패. settlementUuid={}, error={}",
                    settlement.getUuid(), e.getMessage());
            throw e;

        } catch (IllegalStateException e) {
            // 상태 전이 불가 → Skip 처리
            log.error("[Step 2 Processor-Skip] 상태 전이 불가. settlementUuid={}, currentStatus={}, error={}",
                    settlement.getUuid(), settlement.getSettlementStatus(), e.getMessage());
            throw new SettlementValidationException("상태 전이 불가: " + e.getMessage());

        } catch (Exception e) {
            // 예상치 못한 오류 → Skip 처리
            log.error("[Step 2 Processor-Skip] 예상치 못한 오류. settlementUuid={}, error={}",
                    settlement.getUuid(), e.getMessage(), e);
            throw new SettlementValidationException("금액 검증 중 오류 발생: " + e.getMessage());
        }
    }
}
