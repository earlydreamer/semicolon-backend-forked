package dukku.ai.global.tool;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import dukku.common.shared.product.dto.cart.CartItemInternalDto;
import dukku.common.shared.product.out.CartApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartHistoryTool {

    private final CartApiClient cartApiClient;

    @Tool(description = "사용자의 현재 장바구니에 담긴 상품 목록을 조회합니다.")
    public List<String> getCartProducts(ToolContext context) {
        String userId = context.getContext().get("userId").toString();
        log.info("[Tool 호출] 장바구니 상품 조회: userId={}", userId);
        try {
            UUID userUuid = UUID.fromString(userId);
            return cartApiClient.findCartByUserUuid(userUuid)
                    .items()
                    .stream()
                    .map(CartItemInternalDto::productTitle)
                    .toList();
        } catch (Exception e) {
            log.warn("[CartHistoryTool] 장바구니 조회 실패: userId={}, error={}", userId, e.getMessage());
            return List.of();
        }
    }
}
