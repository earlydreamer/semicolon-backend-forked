package dukku.ai.global.tool;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NotificationTool {

    @Tool(description = "사용자에게 추천 상품 알림을 발송한다")
    public String sendNotification(
            @ToolParam(description = "사용자 UUID") UUID userId,
            @ToolParam(description = "추천 상품 목록") List<String> products) {
        log.info("[Tool 호출] 알림 발송: userId={}, products={}", userId, products);
        // TODO: MSA 연동 - notification API 호출
        return "사용자 %s에게 알림 발송 완료 (추천 상품: %s)".formatted(userId, String.join(", ", products));
    }
}
