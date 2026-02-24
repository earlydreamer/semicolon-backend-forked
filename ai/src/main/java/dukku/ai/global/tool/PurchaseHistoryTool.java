package dukku.ai.global.tool;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.chat.model.ToolContext;
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

    @Tool(description = "사용자의 최근 구매 이력을 조회합니다.")
    public List<String> getPurchaseHistory(ToolContext context) {
        String userId = context.getContext().get("userId").toString();
        log.info("[Tool 호출] 구매 이력 조회: userId={}", userId);
        try {
            UUID userUuid = UUID.fromString(userId);
            List<String> productNames = orderApiClient.findOrdersByUserUuid(userUuid, "PAID", 20)
                    .stream()
                    .flatMap(order -> order.getItems().stream())
                    .map(OrderListResponse.SimpleOrderItemResponse::getProductName)
                    .toList();
            return productNames;
        } catch (Exception e) {
            log.warn("[PurchaseHistoryTool] 주문 이력 조회 실패: userId={}, error={}", userId, e.getMessage());
            return List.of();
        }
    }
}
