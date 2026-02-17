package dukku.order.boundedContext.order.app;

import dukku.common.shared.order.exception.OrderNotFoundException;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.entity.ProcessedRefundEvent;
import dukku.order.boundedContext.order.out.OrderRepository;
import dukku.order.boundedContext.order.out.ProcessedRefundEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderSupport {
    private final OrderRepository orderRepository;
    private final ProcessedRefundEventRepository processedRefundEventRepository;

    public Order findOrderByUuid(UUID orderUuid) {
        return orderRepository.findByUuid(orderUuid)
                .orElseThrow(OrderNotFoundException::new);
    }

    public Order findOrderByUuidWithItems(UUID orderUuid) {
        return orderRepository.findByUuidWithItems(orderUuid)
                .orElseThrow(OrderNotFoundException::new);
    }

    public Order save(Order order) {
        return orderRepository.save(order);
    }

    /**
     * 주문 환불 완료 이벤트를 중복 처리하지 않도록 마킹하는 메서드
     *
     * <p>동일한 refundUuid가 이미 처리된 경우 false를 반환</p>
     * <p>처리 이력이 없으면 저장 후 true를 반환</p>
     *
     * @param refundUuid 환불 이벤트 UUID
     * @param orderUuid 주문 UUID
     * @param refundAmount 환불 금액
     * @return 처리 여부
     */
    @Transactional
    public boolean tryMarkRefundCompleted(UUID refundUuid, UUID orderUuid, Long refundAmount) {
        if (processedRefundEventRepository.existsByRefundUuid(refundUuid)) {
            return false;
        }
        processedRefundEventRepository.save(ProcessedRefundEvent.create(refundUuid, orderUuid, refundAmount));
        return true;
    }
}