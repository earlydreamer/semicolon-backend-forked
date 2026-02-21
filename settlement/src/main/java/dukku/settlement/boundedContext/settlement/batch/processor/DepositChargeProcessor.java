package dukku.settlement.boundedContext.settlement.batch.processor;

import dukku.settlement.boundedContext.settlement.entity.Settlement;
import dukku.common.shared.deposit.dto.DepositChargeForSettlementResponse;
import dukku.common.shared.deposit.out.depositApiClient.DepositApiClient;
import dukku.common.shared.settlement.exception.SettlementProcessingException;
import dukku.common.shared.settlement.exception.SettlementValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

/**
 * Step 2: 예치금 충전 Processor
 * - Deposit BC API Client를 통한 예치금 충전 (동기 방식)
 * - 성공 시 Settlement 상태를 SUCCESS로 변경
 * <p>
 * [Idempotency]
 * - 이미 SUCCESS/FAILED 상태인 Settlement는 Skip
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DepositChargeProcessor implements ItemProcessor<Settlement, Settlement> {

    private final DepositApiClient depositApiClient;

    @Override
    public Settlement process(Settlement settlement) throws Exception {
        log.debug("[Step 2 Processor] 예치금 충전 시작. settlementUuid={}, status={}",
                settlement.getUuid(), settlement.getSettlementStatus());

        // Idempotency: 이미 완료된 건은 Skip
        if (settlement.isCompleted()) {
            log.warn("[Step 2 Processor-Skip] 이미 정산 완료된 건. settlementUuid={}", settlement.getUuid());
            return null;
        }

        if (settlement.isFailed()) {
            log.warn("[Step 2 Processor-Skip] 이미 실패 처리된 건. settlementUuid={}", settlement.getUuid());
            return null;
        }

        try {
            DepositChargeForSettlementResponse response = depositApiClient.chargeDepositForSettlement(
                    settlement.getSellerUuid(),
                    settlement.getSettlementAmount(),
                    settlement.getUuid()
            );

            if (response == null || !response.isSuccess()) {
                String errorMsg = response != null ? response.getMessage() : "응답 없음";
                log.error("[Step 2 Processor] 예치금 충전 실패. settlementUuid={}, error={}",
                        settlement.getUuid(), errorMsg);
                settlement.fail();
                throw new SettlementProcessingException("예치금 충전 실패: " + errorMsg);
            }

            settlement.complete();

            log.info("[Step 2 Processor] 예치금 충전 성공. settlementUuid={}, chargedAmount={}, balanceAfter={}",
                    settlement.getUuid(),
                    response.getData().getChargedAmount(),
                    response.getData().getBalanceAfter());

            return settlement;

        } catch (SettlementValidationException | SettlementProcessingException e) {
            // 데이터/비즈니스 오류 → Skip 처리
            log.error("[Step 2 Processor-Skip] settlementUuid={}, error={}",
                    settlement.getUuid(), e.getMessage());
            throw e;

        } catch (DataAccessException e) {
            // DB 접근 오류 → Retry 처리
            log.warn("[Step 2 Processor-Retry] DB 접근 오류. settlementUuid={}, error={}",
                    settlement.getUuid(), e.getMessage());
            throw e;

        } catch (Exception e) {
            // 외부 서비스 연결 실패 포함 모든 예상치 못한 오류 → 즉시 Step 실패
            log.error("[Step 2 Processor] 처리 불가 오류. settlementUuid={}, error={}",
                    settlement.getUuid(), e.getMessage(), e);
            throw e;
        }
    }
}
