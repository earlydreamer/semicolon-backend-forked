package dukku.semicolon.boundedContext.settlement.in.batch.reader;

import dukku.common.shared.order.dto.ConfirmedOrderItemResponse;
import dukku.semicolon.shared.order.out.OrderApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Step 1: 정산 대상 생성을 위한 Reader
 * - Order BC API 호출하여 당일(어제) 구매 확정된 OrderItem 목록 조회
 *
 * [API]
 * - GET /api/v1/internal/orders/items/confirmed?startDateTime={}&endDateTime={}
 * - Response: List<ConfirmedOrderItemResponse>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateSettlementReader {

    private final OrderApiClient orderApiClient;

    /**
     * 어제 구매 확정된 주문 상품 목록을 조회하는 Reader 생성
     * - 배치 실행일 기준 전일 00:00:00 ~ 23:59:59
     */
    public ListItemReader<ConfirmedOrderItemResponse> createReader() {
        // 배치 실행일 기준 전일 (00:00에 실행되므로 어제 확정된 건 조회)
        LocalDate targetDate = LocalDate.now().minusDays(1);
        LocalDateTime startDateTime = targetDate.atStartOfDay();
        LocalDateTime endDateTime = targetDate.atTime(LocalTime.MAX);

        log.info("[Step 1 Reader] 구매 확정 주문 상품 조회 시작. targetDate={}, startDateTime={}, endDateTime={}",
                targetDate, startDateTime, endDateTime);

        // Order BC API 호출
        List<ConfirmedOrderItemResponse> confirmedItems = orderApiClient.findConfirmedItems(startDateTime, endDateTime);

        log.info("[Step 1 Reader] 구매 확정 주문 상품 조회 완료. count={}", confirmedItems.size());

        return new ListItemReader<>(confirmedItems);
    }
}
