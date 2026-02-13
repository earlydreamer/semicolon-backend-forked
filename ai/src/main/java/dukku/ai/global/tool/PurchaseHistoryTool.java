package dukku.ai.global.tool;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PurchaseHistoryTool {

    @Tool(description = "고객의 구매 이력을 조회한다")
    public List<String> getPurchaseHistory(
            @ToolParam(description = "고객 ID") Long customerId) {
        log.info("[Tool 호출] 구매 이력 조회: customerId={}", customerId);
        // TODO: MSA 연동 - user/order 모듈 API 호출
        return List.of("캠핑 의자", "미니 테이블");
    }
}
