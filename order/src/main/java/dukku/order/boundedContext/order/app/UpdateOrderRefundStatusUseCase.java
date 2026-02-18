package dukku.order.boundedContext.order.app;

import dukku.common.shared.order.exception.OrderRefundAmountOutOfRangeException;
import dukku.common.shared.order.exception.OrderRefundRequestInvalidException;
import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateOrderRefundStatusUseCase {
    private final OrderSupport orderSupport;

    /**
     * 결제 환불 이벤트의 환불 금액을 주문에 반영하고 상태를 갱신
     * 누적 환불 금액과 총 결제 금액을 비교해 상태를 분기
     *
     * @param refundUuid 환불 UUID
     * @param orderUuid 주문 UUID
     * @param refundAmount 누적 환불 금액
     */
    @Transactional
    public void updateRefund(UUID refundUuid, UUID orderUuid, Long refundAmount) {
        // 입력값이 없거나 0 이하이면 예외 처리
        if (refundUuid == null || orderUuid == null || refundAmount == null || refundAmount <= 0) {
            throw new OrderRefundRequestInvalidException();
        }

        // 이미 처리된 환불 이벤트면 중복 적용 방지
        if (!orderSupport.tryMarkRefundCompleted(refundUuid, orderUuid, refundAmount)) {
            return;
        }

        // 주문 조회 실패는 상위 트랜잭션에서 롤백 또는 재시도로 처리
        Order order = orderSupport.findOrderByUuid(orderUuid);

        // 환불 금액 범위 방어
        if (refundAmount > Integer.MAX_VALUE) {
            throw new OrderRefundAmountOutOfRangeException();
        }

        // 누적 환불 금액 반영
        order.updateRefundedAmount(refundAmount.intValue());

        // 이미 취소된 주문이면 추가 상태 변경 생략
        if (order.getStatus() == OrderStatus.CANCELED) {
            return;
        }

        // 누적 환불 금액이 총액 이상이면 주문 취소
        if (order.getRefundedAmount() >= order.getTotalAmount()) {
            order.updateOrderStatus(OrderStatus.CANCELED);
            return;
        }

        // 일부만 환불된 경우 부분 환불 상태 반영
        order.updateOrderStatus(OrderStatus.PARTIAL_REFUNDED);
    }
}
