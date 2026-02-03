package dukku.semicolon.boundedContext.deposit.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.deposit.event.DepositChargeFailedEvent;
import dukku.common.shared.deposit.event.DepositChargeSucceededEvent;
import dukku.semicolon.boundedContext.deposit.entity.enums.DepositHistoryType;
import dukku.semicolon.shared.deposit.dto.DepositChargeForSettlementResponse;
import dukku.semicolon.shared.deposit.dto.DepositDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 정산에 의한 예치금 충전 UseCase
 *
 * <p>
 * Internal API에서 호출되는 정산 예치금 충전 로직을 담당한다.
 * settlementUuid를 멱등키로 활용하여 중복 충전을 방지한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChargeDepositForSettlementUseCase {

    private final FindDepositUseCase findDepositUseCase;
    private final IncreaseDepositUseCase increaseDepositUseCase;
    private final FindDepositHistoriesUseCase findDepositHistoriesUseCase;
    private final EventPublisher eventPublisher;

    /**
     * 정산 예치금 충전 실행
     *
     * @param userUuid       충전 대상 사용자 UUID
     * @param amount         충전 금액
     * @param settlementUuid 정산 UUID (멱등키)
     * @return 충전 결과 응답 DTO
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DepositChargeForSettlementResponse execute(UUID userUuid, Long amount, UUID settlementUuid) {
        // 금액 유효성 검증
        if (amount == null || amount <= 0) {
            return DepositChargeForSettlementResponse.failure(
                    "INVALID_AMOUNT", "충전 금액은 0보다 커야 합니다.");
        }

        try {
            // 멱등성 체크: 동일 settlementUuid로 이미 충전된 내역이 있는지 확인
            boolean alreadyCharged = findDepositHistoriesUseCase.existsBySettlementUuid(settlementUuid);
            if (alreadyCharged) {
                log.info("[Internal API] 이미 처리된 정산 충전 요청. settlementUuid={}", settlementUuid);
                // 이미 충전된 경우에도 성공으로 처리 (멱등성)
                DepositDto deposit = findDepositUseCase.findOrCreate(userUuid).toDto();
                return DepositChargeForSettlementResponse.success(
                        deposit.getDepositUuid(), amount, deposit.getBalance());
            }

            // 충전 실행
            increaseDepositUseCase.increase(userUuid, amount, DepositHistoryType.SETTLEMENT, settlementUuid);

            // 성공 이벤트 발행
            eventPublisher.publish(new DepositChargeSucceededEvent(userUuid, amount, settlementUuid));

            // 충전 후 잔액 조회
            DepositDto deposit = findDepositUseCase.findOrCreate(userUuid).toDto();
            log.info("[Internal API] 정산 예치금 충전 성공. userUuid={}, amount={}, settlementUuid={}",
                    userUuid, amount, settlementUuid);

            return DepositChargeForSettlementResponse.success(
                    deposit.getDepositUuid(), amount, deposit.getBalance());

        } catch (Exception e) {
            log.error("[Internal API] 정산 예치금 충전 실패. userUuid={}, amount={}, settlementUuid={}",
                    userUuid, amount, settlementUuid, e);

            // 실패 이벤트 발행
            eventPublisher.publish(new DepositChargeFailedEvent(userUuid, amount, settlementUuid, e.getMessage()));

            return DepositChargeForSettlementResponse.failure(
                    "CHARGE_FAILED", "예치금 충전 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
