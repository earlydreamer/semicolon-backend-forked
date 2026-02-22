package dukku.ai.global.tool;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RecommendationTool {

    @Tool(description = "구매 이력을 기반으로 상품을 추천한다")
    public List<String> recommendProducts(
            @ToolParam(description = "사용자 UUID") UUID userId,
            @ToolParam(description = "구매 이력 목록") List<String> purchaseHistory) {
        log.info("[Tool 호출] 상품 추천: userId={}, history={}", userId, purchaseHistory);
        // TODO: MSA 연동 - product/tag 모듈 기반 추천 알고리즘
        return List.of("프리미엄 캠핑 랜턴", "경량 텐트");
    }
}
