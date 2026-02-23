package dukku.ai.in;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dukku.ai.app.usecase.ProductVectorSyncUseCase;
import dukku.ai.app.usecase.CartRecommendationUseCase;
import dukku.common.shared.product.dto.product.ProductPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * AI 모듈 이벤트 리스너
 * - 장바구니 이벤트: CartSyncEvent를 수신하여 AI 추천을 비동기로 트리거
 * - 상품 이벤트: ProductSyncEvent를 수신하여 VectorStore에 상품 데이터 동기화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiEventListener {

    private final CartRecommendationUseCase cartRecommendationUseCase;
    private final ProductVectorSyncUseCase productVectorSyncUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "cart-events", groupId = "${spring.application.name}-group")
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
            cartRecommendationUseCase.generateRecommendation(userUuid, productTitle);

        } catch (Exception e) {
            log.error("[CartEventListener] 장바구니 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "product-events", groupId = "${spring.application.name}-group")
    public void handleProductSyncEvent(String eventJson) {
        try {
            JsonNode root = objectMapper.readTree(eventJson);
            JsonNode payload = root.get("payload");

            String eventType = payload.get("eventType").asText();
            UUID productUuid = UUID.fromString(payload.get("productUuid").asText());

            ProductPayload productPayload = objectMapper.treeToValue(payload, ProductPayload.class);

            switch (eventType) {
                case "CREATED", "UPDATED" -> productVectorSyncUseCase.upsertProduct(productPayload);
                case "DELETED" -> productVectorSyncUseCase.deleteProduct(productUuid);
                default -> log.warn("[ProductEventListener] 알 수 없는 이벤트 타입: {}", eventType);
            }
        } catch (Exception e) {
            log.error("[ProductEventListener] 상품 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }
}
