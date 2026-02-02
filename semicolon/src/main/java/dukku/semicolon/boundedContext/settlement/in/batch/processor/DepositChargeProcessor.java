package dukku.semicolon.boundedContext.settlement.in.batch.processor;

import dukku.semicolon.boundedContext.settlement.entity.Settlement;
import dukku.semicolon.shared.settlement.exception.SettlementProcessingException;
import dukku.semicolon.shared.settlement.exception.SettlementValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Step 3: 예치금 충전 Processor
 * - Deposit BC API Client를 통한 예치금 충전 (동기 방식)
 * - 성공 시 Settlement 상태를 SUCCESS로 변경
 *
 * [TODO] Deposit BC API Client 구현 필요
 * - DepositFacade를 직접 참조하지 않고 API Client를 통해 호출해야 함
 * - Bounded Context 간 직접 참조 금지 원칙 준수
 *
 * [Idempotency]
 * - 이미 SUCCESS/FAILED 상태인 Settlement는 Skip
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DepositChargeProcessor implements ItemProcessor<Settlement, Settlement> {

    // TODO: DepositApiClient 구현 후 주입
    // private final DepositApiClient depositApiClient;

    @Override
    public Settlement process(Settlement settlement) throws Exception {
        log.debug("[Step 3 Processor] 예치금 충전 시작. settlementUuid={}, status={}",
                settlement.getUuid(), settlement.getSettlementStatus());

        // Idempotency: 이미 완료된 건은 Skip
        if (settlement.isCompleted()) {
            log.warn("[Step 3 Processor-Skip] 이미 정산 완료된 건. settlementUuid={}", settlement.getUuid());
            return null; // Writer로 전달하지 않음
        }

        if (settlement.isFailed()) {
            log.warn("[Step 3 Processor-Skip] 이미 실패 처리된 건. settlementUuid={}", settlement.getUuid());
            return null;
        }

        try {
            // TODO: Deposit BC API Client 구현 필요
            // DepositFacade 직접 참조 대신 API Client를 통해 호출
            //
            // depositApiClient.chargeDepositForSettlement(
            //     settlement.getSellerUuid(),
            //     settlement.getSettlementAmount(),
            //     settlement.getUuid()
            // );
            //
            // API 예시:
            // POST /api/v1/internal/deposits/{sellerUuid}/charge
            // Request Body: { "amount": 10000, "settlementUuid": "uuid" }
            // Response: { "success": true, "depositUuid": "uuid" }

            log.warn("[TODO] Deposit BC API Client 미구현. 예치금 충전 스킵됨. settlementUuid={}",
                    settlement.getUuid());

            // TODO: API 호출 성공 후 SUCCESS 상태로 변경
            // settlement.complete();

            // TODO: API 구현 전까지 임시로 null 반환 (Skip)
            return null;

        } catch (SettlementValidationException e) {
            // 데이터 유효성 오류 → Skip 처리
            log.error("[Step 3 Processor-Skip] 데이터 유효성 오류. settlementUuid={}, error={}",
                    settlement.getUuid(), e.getMessage());
            throw e;

        } catch (SettlementProcessingException e) {
            // 비즈니스 처리 오류 → Skip 처리
            log.error("[Step 3 Processor-Skip] 비즈니스 처리 오류. settlementUuid={}, error={}",
                    settlement.getUuid(), e.getMessage());
            throw e;

        } catch (DataAccessException e) {
            // DB 접근 오류 → Retry 처리
            log.warn("[Step 3 Processor-Retry] DB 접근 오류. settlementUuid={}, error={}",
                    settlement.getUuid(), e.getMessage());
            throw e;

        } catch (Exception e) {
            // 예상치 못한 오류 → Skip 처리
            log.error("[Step 3 Processor-Skip] 예상치 못한 오류. settlementUuid={}, error={}",
                    settlement.getUuid(), e.getMessage(), e);
            throw new SettlementProcessingException(
                    "예치금 충전 중 오류 발생: " + e.getMessage());
        }
    }
}
