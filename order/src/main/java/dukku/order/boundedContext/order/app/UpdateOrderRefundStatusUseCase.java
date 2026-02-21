package dukku.order.boundedContext.order.app;

import dukku.common.shared.order.exception.OrderRefundAmountOutOfRangeException;
import dukku.common.shared.order.exception.OrderRefundRequestInvalidException;
import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.out.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateOrderRefundStatusUseCase {
    private final OrderSupport orderSupport;
    private final ReturnRequestRepository returnRequestRepository;

    /**
     * 결제 환불 이벤트의 환불 금액을 주문에 반영하고 상태를 갱신
     * 누적 환불 금액과 총 결제 금액을 비교해 상태를 분기
     *
     * @param refundUuid        환불 UUID
     * @param orderUuid         주문 UUID
     * @param refundAmount      누적 환불 금액
     * @param refundedItemUuids 환불 처리된 아이템의 UUID 목록
     */
    @Transactional
    public void updateRefund(UUID refundUuid, UUID orderUuid, Long refundAmount, List<UUID> refundedItemUuids) {
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

        // 개별 상품 환불 상태 처리 및 반품 묶음(ReturnRequest) 상태 갱신
        if (refundedItemUuids != null && !refundedItemUuids.isEmpty()) {
            order.getOrderItems().forEach(item -> {
                if (refundedItemUuids.contains(item.getUuid())) {
                    item.updateOrderStatus(dukku.common.shared.order.type.OrderItemStatus.REFUND_COMPLETED);
                }
            });

            returnRequestRepository.findByOrderUuid(orderUuid).forEach(req -> {
                if (req.getStatus() == dukku.common.shared.order.type.ReturnStatus.RETURN_APPROVED) {
                    boolean allRefunded = req.getReturnItems().stream()
                            .allMatch(ri -> ri.getOrderItem()
                                    .getStatus() == dukku.common.shared.order.type.OrderItemStatus.REFUND_COMPLETED);
                    if (allRefunded) {
                        req.complete();
                    }
                }
            });
        }

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

    /**
     * PG 결제 취소 실패 등으로 인한 환불 실패 시 보상 트랜잭션 수행
     *
     * @param orderUuid 주문 UUID
     */
    @Transactional
    public void failRefund(UUID orderUuid) {
        if (orderUuid == null) {
            return;
        }

        // 환불 실패 시 반품 승인 대기 중이던 ReturnRequest를 거절(실패) 상태로 변경하고,
        // 연관된 OrderItem 상태를 이전(DELIVERED) 상태로 변경
        returnRequestRepository.findByOrderUuid(orderUuid).forEach(req -> {
            if (req.getStatus() == dukku.common.shared.order.type.ReturnStatus.RETURN_APPROVED) {
                req.reject(); // RETURN_REJECTED 상태로 변경

                req.getReturnItems().forEach(ri -> {
                    dukku.common.shared.order.type.OrderItemStatus currentStatus = ri.getOrderItem().getStatus();
                    if (currentStatus == dukku.common.shared.order.type.OrderItemStatus.REFUND_REQUESTED
                            || currentStatus == dukku.common.shared.order.type.OrderItemStatus.REFUND_IN_PROGRESS) {
                        ri.getOrderItem().updateOrderStatus(dukku.common.shared.order.type.OrderItemStatus.DELIVERED);
                    }
                });
            }
        });
    }
}
