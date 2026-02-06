package dukku.semicolon.boundedContext.settlement.app;

import dukku.semicolon.boundedContext.settlement.entity.Settlement;
import dukku.semicolon.shared.deposit.dto.DepositChargeForSettlementResponse;
import dukku.semicolon.shared.deposit.out.depositApiClient.DepositApiClient;
import dukku.semicolon.shared.settlement.exception.SettlementProcessingException;
import dukku.semicolon.shared.settlement.exception.SettlementValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 정산 수동 예치금 충전 요청 UseCase
 * - Settlement 상태: PENDING → PROCESSING → SUCCESS
 * - 관리자가 정산을 수동으로 예치금 충전 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ManualProcessSettlementUseCase {

    private final SettlementSupport settlementSupport;
    private final DepositApiClient depositApiClient;

    @Transactional
    public Settlement execute(UUID settlementUuid) {
        log.info("[수동 정산 처리] settlementUuid={}", settlementUuid);

        try {
            // 1. Settlement 조회
            Settlement settlement = settlementSupport.findByUuid(settlementUuid);

            // 2. Settlement 상태 전이 검증 및 PROCESSING으로 변경
            settlement.startProcessing();
            settlementSupport.save(settlement);

            log.info("[수동 정산 처리] PROCESSING 상태로 변경. settlementUuid={}", settlementUuid);

            // 3. Deposit API Client를 통한 예치금 충전 처리
            DepositChargeForSettlementResponse response = depositApiClient.chargeDepositForSettlement(
                    settlement.getSellerUuid(),
                    settlement.getSettlementAmount(),
                    settlement.getUuid()
            );

            log.info("[수동 정산 처리] 예치금 충전 완료. settlementUuid={}, depositUuid={}, chargedAmount={}",
                    settlementUuid, response.getData().getDepositUuid(), response.getData().getChargedAmount());

            // 4. Settlement 상태를 SUCCESS로 변경
            settlement.complete();
            settlementSupport.save(settlement);

            log.info("[수동 정산 처리 완료] settlementUuid={}, status={}", settlementUuid, settlement.getSettlementStatus());

            return settlement;

        } catch (IllegalStateException e) {
            log.error("[수동 정산 처리 실패] 상태 전이 불가. settlementUuid={}, error={}", settlementUuid, e.getMessage());
            throw SettlementValidationException.invalidStatusTransition(
                    "CURRENT", "PROCESSING");

        } catch (SettlementValidationException e) {
            log.error("[수동 정산 처리 실패] 데이터 유효성 오류. settlementUuid={}, error={}", settlementUuid, e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("[수동 정산 처리 실패] 예상치 못한 오류. settlementUuid={}, error={}", settlementUuid, e.getMessage(), e);
            throw SettlementProcessingException.externalServiceFailed(
                    "DepositApiClient", e.getMessage());
        }
    }
}
