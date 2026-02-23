package dukku.ai.global.tool;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NotificationTool {

    @Tool(description = "사용자에게 추천 상품 알림을 발송한다")
    public String sendNotification(
            ToolContext context,
            @ToolParam(description = "추천 상품 목록") List<String> products) {
        String userId = context.getContext().get("userId").toString();
        log.info("[Tool 호출] 알림 발송: userId={}, products={}", userId, products);
        try {
            UUID userUuid = UUID.fromString(userId);
            // TODO: MSA 연동 - notification API 호출
            return "사용자 %s에게 알림 발송 완료 (추천 상품: %s)".formatted(userUuid, String.join(", ", products));
        } catch (IllegalArgumentException e) {
            log.warn("[NotificationTool] 유효하지 않은 UUID 형식: userId={}", userId);
            return "유효하지 않은 사용자 ID 입니다.";
        }
    }
}
