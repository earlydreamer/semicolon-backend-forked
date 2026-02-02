package dukku.semicolon.boundedContext.settlement.in.batch.writer;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.settlement.event.SettlementCompletedEvent;
import dukku.semicolon.boundedContext.settlement.entity.Settlement;
import dukku.semicolon.boundedContext.settlement.out.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Step 2: 예치금 충전 완료된 Settlement 저장 Writer (현재)
 * - SUCCESS 상태로 변경된 Settlement 저장
 * - 정산 완료 이벤트 발행 (Order BC로)
 *
 * [TODO] Deposit BC API Client 구현 후 활성화
 * [TODO] Order BC API 구현 시 Step 3 Writer로 변경됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DepositChargeWriter implements ItemWriter<Settlement> {

    private final SettlementRepository settlementRepository;
    private final EventPublisher eventPublisher;

    @Override
    public void write(Chunk<? extends Settlement> chunk) throws Exception {
        log.info("[Step 2 Writer] SUCCESS 상태 Settlement 저장 시작 - 건수: {}", chunk.size());

        for (Settlement settlement : chunk) {
            // 1. Settlement 저장
            settlementRepository.save(settlement);
            log.debug("[Step 2 Writer] Settlement 상태 업데이트 완료 - UUID: {}, 상태: {}",
                    settlement.getUuid(),
                    settlement.getSettlementStatus());

            // 2. 정산 완료 이벤트 발행 (Order BC로)
            // - Order BC에서 해당 OrderItem의 상태를 변경하는 데 사용
            if (settlement.isCompleted()) {
                publishSettlementCompletedEvent(settlement);
            }
        }

        log.info("[Step 2 Writer] SUCCESS 상태 Settlement 저장 완료 - 총 {}건 처리됨", chunk.size());
    }

    /**
     * 정산 완료 이벤트 발행
     * - Order BC에서 해당 OrderItem의 상태를 변경
     */
    private void publishSettlementCompletedEvent(Settlement settlement) {
        try {
            SettlementCompletedEvent event = new SettlementCompletedEvent(
                    settlement.getUuid(),
                    settlement.getOrderItemId(),
                    settlement.getSellerUuid(),
                    settlement.getSettlementAmount(),
                    settlement.getCompletedAt()
            );

            eventPublisher.publish(event);

            log.info("[이벤트 발행] 정산 완료 이벤트 발행 완료. settlementUuid={}, orderItemUuid={}",
                    settlement.getUuid(), settlement.getOrderItemId());

        } catch (Exception e) {
            log.error("[이벤트 발행 실패] 정산 완료 이벤트 발행 실패. settlementUuid={}, error={}",
                    settlement.getUuid(), e.getMessage(), e);
            // 이벤트 발행 실패는 배치 실패로 이어지지 않도록 예외를 삼킴
            // TODO: 이벤트 발행 실패 시 재발행 로직 추가 고려
        }
    }
}
