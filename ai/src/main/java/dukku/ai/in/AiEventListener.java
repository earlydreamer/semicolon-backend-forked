package dukku.ai.in;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dukku.ai.app.usecase.InitializeAiUserMemoryUseCase;
import dukku.ai.app.usecase.ProductVectorSyncUseCase;
import dukku.ai.app.usecase.CartRecommendationUseCase;
import dukku.ai.app.usecase.PurchaseMemoryUseCase;
import dukku.common.shared.order.event.OrderPaidEvent;
import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.product.dto.product.ProductPayload;

import dukku.common.shared.user.event.UserAiInitializationFailedEvent;
import dukku.common.shared.user.event.UserProductInitializedEvent;
import dukku.common.shared.product.event.CartSyncEvent;
import dukku.common.shared.product.type.CartEventType;
import dukku.common.shared.product.dto.cart.CartItemAddedPayload;
import dukku.common.shared.product.event.ProductSyncEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AI 모듈 이벤트 리스너
 * - 장바구니 이벤트: CartSyncEvent를 수신하여 AI 추천을 비동기로 트리거
 * - 상품 이벤트: ProductSyncEvent를 수신하여 product_search 테이블에 상품 데이터 동기화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiEventListener {

    private final InitializeAiUserMemoryUseCase initializeAiUserMemoryUseCase;
    private final EventPublisher eventPublisher;
    private final CartRecommendationUseCase cartRecommendationUseCase;
    private final PurchaseMemoryUseCase purchaseMemoryUseCase;
    private final ProductVectorSyncUseCase productVectorSyncUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user.product-initialized", groupId = "${spring.application.name}-group")
    public void handleProductInitialized(UserProductInitializedEvent event) {
        try {
            initializeAiUserMemoryUseCase.execute(event.userUuid(), event.nickname(), event.email());
            log.info("[UserProductInitializedEvent] AI user initialization completed. userUuid={}", event.userUuid());
        } catch (Exception e) {
            String reason = e.getMessage() == null ? "AI user initialization exception" : e.getMessage();
            eventPublisher.publish(new UserAiInitializationFailedEvent(event.userUuid(), reason));
            log.error("[UserProductInitializedEvent] AI user 초기화 실패. userUuid={}", event.userUuid());
        }
    }

    @KafkaListener(topics = "cart-events", groupId = "${spring.application.name}-group")
    public void handleCartEvent(CartSyncEvent event) {
        try {
            if (event.eventType() != CartEventType.ITEM_ADDED) {
                return;
            }

            if (event.payload() instanceof CartItemAddedPayload payload) {
                UUID userUuid = payload.userUuid();
                String productTitle = payload.productTitle();

                log.info("[CartEventListener] 장바구니 추가 이벤트 수신: userId={}, product={}", userUuid, productTitle);

                // 비동기로 추천 생성
                cartRecommendationUseCase.generateRecommendation(userUuid, productTitle);
            }
        } catch (Exception e) {
            log.error("[CartEventListener] 장바구니 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "order.paid", groupId = "${spring.application.name}-group")
    public void handleOrderPaidEvent(OrderPaidEvent event) {
        try {
            UUID userUuid = event.userUuid();
            List<OrderPaidEvent.PaidItem> items = event.items();

            log.info("[OrderPaidEvent] 결제 완료 이벤트 수신: userUuid={}, itemCount={}", userUuid, items.size());

            purchaseMemoryUseCase.storePurchaseMemory(userUuid, items);

        } catch (Exception e) {
            log.error("[OrderPaidEvent] 결제 완료 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "product-events", groupId = "${spring.application.name}-group")
    public void handleProductSyncEvent(ProductSyncEvent event) {
        try {
            ProductPayload productPayload = event.payload();
            String eventType = productPayload.eventType().name();
            UUID productUuid = productPayload.productUuid();

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
