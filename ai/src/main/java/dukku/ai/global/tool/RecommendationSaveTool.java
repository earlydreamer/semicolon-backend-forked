package dukku.ai.global.tool;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RecommendationSaveTool {

    @Tool(description = "추천 결과를 DB에 저장한다")
    public String saveRecommendation(
            @ToolParam(description = "고객 ID") Long customerId,
            @ToolParam(description = "추천 상품 목록") List<String> products) {
        log.info("[Tool 호출] 추천 결과 저장: customerId={}, products={}", customerId, products);
        // TODO: MSA 연동 - recommendation 저장 API 호출
        return "고객 %d의 추천 결과 %d건 저장 완료".formatted(customerId, products.size());
    }
}
