package dukku.ai.global.tool;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import dukku.common.shared.order.dto.OrderListResponse;
import dukku.common.shared.order.out.OrderApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseHistoryTool {

    private final OrderApiClient orderApiClient;

    @Tool(description = "사용자의 구매 이력을 조회하여 구매한 상품명 목록을 반환한다")
    public List<String> getPurchaseHistory(
            @ToolParam(description = "사용자 UUID") UUID userId) {
        log.info("[Tool 호출] 구매 이력 조회: userId={}", userId);
        try {
            return orderApiClient.findOrdersByUserUuid(userId, "PAID", 20)
                    .stream()
                    .flatMap(order -> order.getItems().stream())
                    .map(OrderListResponse.SimpleOrderItemResponse::getProductName)
                    .toList();
        } catch (Exception e) {
            log.warn("[PurchaseHistoryTool] 주문 이력 조회 실패: userId={}, error={}", userId, e.getMessage());
            return List.of();
        }
    }
}
