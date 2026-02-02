package dukku.semicolon.boundedContext.settlement.in.batch.reader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Step 1: 정산 대상 생성을 위한 Reader
 * - Order BC API 호출하여 당일(어제) 구매 확정된 OrderItem 목록 조회
 *
 * [TODO] Order BC API 구현 필요:
 * - GET /api/v1/internal/orders/items/confirmed?date={date}
 * - Response: List<ConfirmedOrderItemDto>
 *
 * [TODO] ConfirmedOrderItemDto 구현 필요 (shared/order/dto)
 * - orderItemUuid, orderUuid, buyerUuid, sellerUuid, productUuid
 * - productName, productPrice, confirmedAt
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateSettlementReader {

    // TODO: Order BC API Client 구현 후 주입
    // private final OrderApiClient orderApiClient;

    /*
    TODO: Order BC API 구현 후 활성화

    public ListItemReader<ConfirmedOrderItemDto> createReader() {
        // 배치 실행일 기준 전일 (00:00에 실행되므로 어제 확정된 건 조회)
        LocalDate targetDate = LocalDate.now().minusDays(1);

        log.info("[Step 1 Reader] 구매 확정 주문 상품 조회 시작. targetDate={}", targetDate);

        // Order BC API 호출
        List<ConfirmedOrderItemDto> confirmedItems = orderApiClient.getConfirmedOrderItems(targetDate);

        log.info("[Step 1 Reader] 구매 확정 주문 상품 조회 완료. count={}", confirmedItems.size());

        return new ListItemReader<>(confirmedItems);
    }
    */
}
