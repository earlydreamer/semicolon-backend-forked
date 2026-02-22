package dukku.ai.in;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dukku.ai.app.usecase.CartRecommendationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 장바구니 이벤트 수신 리스너
 * Product 모듈의 CreateCartUseCase가 발행하는 CartSyncEvent를 수신하여
 * AI 추천을 비동기로 트리거합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiEventListener {

    private final CartRecommendationUseCase cartRecommendationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "cart.item-added", groupId = "${spring.application.name}-group")
    public void handleCartEvent(String eventJson) {
        try {
            JsonNode root = objectMapper.readTree(eventJson);
            String eventType = root.get("eventType").asText();

            if (!"ITEM_ADDED".equals(eventType)) {
                return;
            }

            JsonNode payload = root.get("payload");
            UUID userUuid = UUID.fromString(payload.get("userUuid").asText());
            String productTitle = payload.get("productTitle").asText();

            log.info("[CartEventListener] 장바구니 추가 이벤트 수신: userId={}, product={}", userUuid, productTitle);

            // 비동기로 추천 생성
            cartRecommendationService.generateRecommendation(userUuid, productTitle);

        } catch (Exception e) {
            log.error("[CartEventListener] 장바구니 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }
}
