package dukku.order.boundedContext.order.app.scheduler;

import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.app.UpdateOrderStatusUseCase;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingOrderExpirationScheduler {

    private final OrderRepository orderRepository;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @Value("${order.pending-expiration.minutes:5}")
    private long pendingExpirationMinutes;

    @Scheduled(cron = "${order.pending-expiration.cron:0 */5 * * * *}")
    // 만료 기준을 지난 PENDING 주문을 실패 처리해 예약 상품을 해제한다.
    public void expirePendingOrders() {
        expirePendingOrdersNow();
    }

    public int expirePendingOrdersNow() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(pendingExpirationMinutes);
        List<Order> expiredPendingOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, cutoff);

        if (expiredPendingOrders.isEmpty()) {
            return 0;
        }

        log.info("[PendingOrderExpiration] expiring {} orders. cutoff={}", expiredPendingOrders.size(), cutoff);

        for (Order order : expiredPendingOrders) {
            try {
                updateOrderStatusUseCase.failPayment(order.getUuid());
            } catch (Exception e) {
                log.error("[PendingOrderExpiration] failed to expire orderUuid={}", order.getUuid(), e);
            }
        }

        return expiredPendingOrders.size();
    }
}