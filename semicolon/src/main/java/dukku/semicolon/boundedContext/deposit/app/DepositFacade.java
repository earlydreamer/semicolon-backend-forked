package dukku.semicolon.boundedContext.deposit.app;

import dukku.semicolon.boundedContext.deposit.entity.DepositHistory;
import dukku.semicolon.boundedContext.deposit.entity.enums.DepositHistoryType;
import dukku.semicolon.shared.deposit.dto.DepositDto;
import dukku.semicolon.shared.deposit.dto.DepositHistoryDto;
import dukku.semicolon.shared.deposit.dto.DepositChargeForSettlementResponse;
import dukku.common.shared.payment.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Deposit 도메인 Facade
 *
 * <p>
 * Controller가 호출하는 진입점. 각 UseCase로 위임만 수행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DepositFacade {

    private final FindDepositUseCase findDepositUseCase;
    private final IncreaseDepositUseCase increaseDepositUseCase;
    private final DecreaseDepositUseCase decreaseDepositUseCase;
    private final FindDepositHistoriesUseCase findDepositHistoriesUseCase;
    private final DeductDepositForPaymentUseCase deductDepositForPaymentUseCase;
    private final RefundDepositUseCase refundDepositUseCase;
    private final ChargeDepositUseCase chargeDepositUseCase;
    private final ChargeDepositForSettlementUseCase chargeDepositForSettlementUseCase;

    /**
     * 사용자 예치금 조회
     */
    public DepositDto findDeposit(UUID userUuid) {
        return findDepositUseCase.findOrCreate(userUuid).toDto();
    }

    /**
     * 예치금 잔액 증가 (충전/정산/환불)
     */
    public void increaseDeposit(UUID userUuid, Long amount, DepositHistoryType type, UUID orderItemUuid) {
        increaseDepositUseCase.increase(userUuid, amount, type, orderItemUuid);
    }

    /**
     * 예치금 잔액 차감 (사용/롤백)
     */
    public void decreaseDeposit(UUID userUuid, Long amount, DepositHistoryType type, UUID orderItemUuid) {
        decreaseDepositUseCase.decrease(userUuid, amount, type, orderItemUuid);
    }

    /**
     * 예치금 변동 내역 조회 (커서 기반 페이징)
     */
    public Slice<DepositHistoryDto> findHistories(UUID userUuid, Integer cursor, int size) {
        Slice<DepositHistory> histories = findDepositHistoriesUseCase.findHistories(userUuid, cursor, size);
        List<DepositHistoryDto> content = histories.getContent().stream()
                .map(DepositHistory::toDto)
                .toList();
        return new SliceImpl<>(content, histories.getPageable(), histories.hasNext());
    }

    /**
     * 전체 예치금 변동 내역 조회 (관리자용, 커서 기반 페이징)
     */
    public Slice<DepositHistoryDto> findAllHistories(Integer cursor, int size) {
        Slice<DepositHistory> histories = findDepositHistoriesUseCase.findAllHistories(cursor, size);
        List<DepositHistoryDto> content = histories.getContent().stream()
                .map(DepositHistory::toDto)
                .toList();
        return new SliceImpl<>(content, histories.getPageable(), histories.hasNext());
    }

    /**
     * @deprecated Use {@link #findHistories(UUID, Integer, int)} instead.
     */
    @Deprecated
    public List<DepositHistoryDto> findHistories(UUID userUuid) {
        return findDepositHistoriesUseCase.findHistories(userUuid).stream()
                .map(DepositHistory::toDto)
                .toList();
    }

    /**
     * @deprecated Use {@link #findAllHistories(Integer, int)} instead.
     */
    @Deprecated
    public List<DepositHistoryDto> findAllHistories() {
        return findDepositHistoriesUseCase.findAllHistories().stream()
                .map(DepositHistory::toDto)
                .toList();
    }

    /**
     * 결제에 따른 예치금 차감 (Saga 참여)
     */
    public void deductDepositForPayment(UUID userUuid, Long totalAmount, UUID orderUuid,
            List<PaymentSuccessEvent.ItemDepositUsage> itemDepositUsages) {
        deductDepositForPaymentUseCase.execute(userUuid, totalAmount, orderUuid, itemDepositUsages);
    }

    /**
     * 환불 처리 (Saga 참여)
     */
    public void refundDeposit(UUID userUuid, Long amount, UUID orderUuid) {
        refundDepositUseCase.execute(userUuid, amount, orderUuid);
    }

    /**
     * 정산에 의한 예치금 충전 (Saga 참여)
     *
     * @deprecated {@link #chargeDepositForSettlementApi(UUID, Long, UUID)} 사용을
     *             권장합니다.
     */
    @Deprecated
    public void chargeDepositForSettlement(UUID userUuid, Long amount, UUID settlementUuid) {
        log.warn(
                "[DEPRECATED] 이벤트 기반 정산 충전 로직(chargeDepositForSettlement)이 호출되었습니다. API 방식(chargeDepositForSettlementApi)으로의 전환이 필요합니다. settlementUuid={}",
                settlementUuid);
        chargeDepositUseCase.execute(userUuid, amount, settlementUuid);
    }

    /**
     * Internal API용 정산 예치금 충전
     */
    public DepositChargeForSettlementResponse chargeDepositForSettlementApi(
            UUID userUuid, Long amount, UUID settlementUuid) {
        return chargeDepositForSettlementUseCase.execute(userUuid, amount, settlementUuid);
    }
}
