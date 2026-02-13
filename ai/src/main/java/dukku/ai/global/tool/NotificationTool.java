package dukku.ai.global.tool;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NotificationTool {

    @Tool(description = "고객에게 추천 상품 알림을 발송한다")
    public String sendNotification(
            @ToolParam(description = "고객 ID") Long customerId,
            @ToolParam(description = "추천 상품 목록") List<String> products) {
        log.info("[Tool 호출] 알림 발송: customerId={}, products={}", customerId, products);
        // TODO: MSA 연동 - notification API 호출
        return "고객 %d에게 알림 발송 완료 (추천 상품: %s)".formatted(customerId, String.join(", ", products));
    }
}
