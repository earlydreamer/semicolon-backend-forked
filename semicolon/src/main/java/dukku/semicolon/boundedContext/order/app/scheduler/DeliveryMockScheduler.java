package dukku.semicolon.boundedContext.order.app.scheduler;

import dukku.common.shared.order.type.OrderItemStatus;
import dukku.semicolon.boundedContext.order.entity.OrderItem;
import dukku.semicolon.boundedContext.order.out.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryMockScheduler {
    private final OrderItemRepository orderItemRepository;

    /**
     * 가상 배송 시뮬레이션
     * 주기: 1시간마다 실행 (매 정각)
     * 예: 13:00, 14:00, 15:00 ...
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void runDeliverySimulation() {
        log.info("[Mock Delivery] Simulation Started... (Realistic Time Scale)");

        // 1. 배송시작(SHIPPED) -> 이동중(IN_TRANSIT)
        // 상황: 판매자가 송장 입력 후 택배사가 수거하여 허브로 이동
        // 소요: 4시간
        processDeliveryStep(
                OrderItemStatus.SHIPPED,
                OrderItemStatus.IN_TRANSIT,
                4
        );

        // 2. 이동중(IN_TRANSIT) -> 배송출발(OUT_FOR_DELIVERY)
        // 상황: 허브에서 분류(야간 작업) 후 지역 터미널로 이동하여 기사님 배정
        // 소요: 12시간 (가장 오래 걸리는 구간)
        processDeliveryStep(
                OrderItemStatus.IN_TRANSIT,
                OrderItemStatus.OUT_FOR_DELIVERY,
                12
        );

        // 3. 배송출발(OUT_FOR_DELIVERY) -> 배송완료(DELIVERED)
        // 상황: 기사님이 물건을 싣고 고객에게 전달
        // 소요: 6시간
        processDeliveryStep(
                OrderItemStatus.OUT_FOR_DELIVERY,
                OrderItemStatus.DELIVERED,
                6
        );

        log.info("[Mock Delivery] Simulation Finished.");
    }

    private void processDeliveryStep(OrderItemStatus currentStatus, OrderItemStatus nextStatus, int hoursThreshold) {
        // 기준 시간 계산: 현재 시간 - N시간
        LocalDateTime thresholdTime = LocalDateTime.now().minusHours(hoursThreshold);

        // 조건에 맞는 아이템 조회 (JPA)
        List<OrderItem> targets = orderItemRepository.findAllByStatusAndDeliveryDateBefore(currentStatus, thresholdTime);

        if (targets.isEmpty()) return;

        int count = 0;
        for (OrderItem item : targets) {
            // 상태 변경 및 시간 갱신
            // (updateMockDeliveryStatus 메서드에서 deliveryDate를 now()로 갱신해야 다음 단계 시간 계산이 맞음)
            item.updateMockDeliveryStatus(nextStatus);
            count++;
        }

        log.info("[Update Status] {} -> {} (Elapsed: {}h, Count: {})", currentStatus, nextStatus, hoursThreshold, count);
    }
}