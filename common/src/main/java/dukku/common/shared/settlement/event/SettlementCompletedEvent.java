package dukku.common.shared.settlement.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 정산 완료 이벤트
 * - Settlement가 SUCCESS 상태로 변경되면 발행
 * - Order BC에서 해당 OrderItem의 상태를 변경하는 데 사용
 *
 * @param settlementUuid 정산 UUID
 * @param orderItemUuid 주문 상품 UUID
 * @param sellerUuid 판매자 UUID
 * @param settlementAmount 정산 금액 (수수료 제외)
 * @param completedAt 정산 완료 일시
 */
public record SettlementCompletedEvent(
        UUID settlementUuid,
        UUID orderItemUuid,
        UUID sellerUuid,
        Long settlementAmount,
        LocalDateTime completedAt
) {
}
