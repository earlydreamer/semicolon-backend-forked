package dukku.order.boundedContext.order.app;

import dukku.order.boundedContext.order.entity.Order;
import dukku.common.shared.order.type.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateOrderRefundStatusUseCase {
    private final OrderSupport orderSupport;

    /**
     * 결제/환불 이벤트의 환불금액 반영과 주문 상태 정리
     * 총 결제금액 대비 누적 환불금액 기준으로 상태 분기
     */
    @Transactional
    public void updateRefund(UUID orderUuid, Long refundAmount) {
        // 입력값이 null 또는 0 이하면 즉시 종료
        if (refundAmount == null || refundAmount <= 0) {
            return;
        }

        // 주문 조회 실패는 상위 트랜잭션에서 롤백/재시도 대상으로 이동
        Order order = orderSupport.findOrderByUuid(orderUuid);

        // 환불금액 범위 방어
        if (refundAmount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Refund amount exceeds supported integer range.");
        }

        // 누적 환불액 반영(총액 초과 방지)
        order.updateRefundedAmount(refundAmount.intValue());

        // 주문 상태가 이미 취소면 추가 상태 변경 생략
        if (order.getStatus() == OrderStatus.CANCELED) {
            return;
        }

        // 누적 환불액 >= 총액이면 주문 취소
        if (order.getRefundedAmount() >= order.getTotalAmount()) {
            order.updateOrderStatus(OrderStatus.CANCELED);
            return;
        }

        // 누적 환불액 미만이면 부분환불
        order.updateOrderStatus(OrderStatus.PARTIAL_REFUNDED);
    }
}
