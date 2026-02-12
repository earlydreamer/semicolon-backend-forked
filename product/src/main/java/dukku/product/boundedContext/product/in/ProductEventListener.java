package dukku.product.boundedContext.product.in;

import dukku.common.shared.order.event.OrderProductSaleConfirmedEvent;
import dukku.common.shared.order.event.OrderProductSaleReleasedEvent;
import dukku.common.shared.product.event.ProductStatsBulkUpdatedEvent;
import dukku.product.boundedContext.product.app.cqrs.ProductSyncFacade;
import dukku.product.boundedContext.product.app.cqrs.SaveToElasticSearchUseCase;
import dukku.product.boundedContext.product.app.cqrs.SyncSearchProductStatsUseCase;
import dukku.product.boundedContext.product.app.usecase.product.ConfirmProductSaleUseCase;
import dukku.product.boundedContext.product.app.usecase.product.ReleaseProductReservationUseCase;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.out.ProductRepository;
import dukku.product.global.event.ProductCreatedEvent;
import dukku.product.global.event.ProductDeletedEvent;
import dukku.product.global.event.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductEventListener {
    private final ProductSyncFacade productSyncFacade;
    private final SaveToElasticSearchUseCase saveToElasticSearchUseCase;
    private final SyncSearchProductStatsUseCase syncSearchProductStatsUseCase;
    private final ConfirmProductSaleUseCase confirmProductSaleUseCase;
    private final ReleaseProductReservationUseCase releaseProductReservationUseCase;

    private final ProductRepository productRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    // 1. 생성 동기화
    @KafkaListener(topics = "product.created", groupId = "${spring.application.name}-group")
    public void syncCreate(String eventJson) {
        try {
            ProductCreatedEvent event = objectMapper.readValue(eventJson, ProductCreatedEvent.class);
            Product product = productRepository.findById(event.productId())
                    .orElseThrow(); // Or handle gracefully
            saveToElasticSearchUseCase.execute(product, true);
        } catch (Exception e) {
            log.error("Failed to process product.created event", e);
        }
    }

    // 2. 수정 동기화
    @KafkaListener(topics = "product.updated", groupId = "${spring.application.name}-group")
    public void syncUpdate(String eventJson) {
        try {
            ProductUpdatedEvent event = objectMapper.readValue(eventJson, ProductUpdatedEvent.class);
            log.info("Sync Update Product: {}", event.productId());

            Product product = productRepository.findById(event.productId())
                    .orElseThrow();
            saveToElasticSearchUseCase.execute(product, event.isCategoryChanged());
        } catch (Exception e) {
            log.error("Failed to process product.updated event", e);
        }
    }

    // 3. 삭제 동기화
    @KafkaListener(topics = "product.deleted", groupId = "${spring.application.name}-group")
    public void syncDelete(String eventJson) {
        try {
            ProductDeletedEvent event = objectMapper.readValue(eventJson, ProductDeletedEvent.class);
            productSyncFacade.syncProductToElasticsearch(event.productId().longValue());
        } catch (Exception e) {
            log.error("Failed to process product.deleted event", e);
        }
    }

    // 4. 통계 동기화 (배치 작업 후 실행)
    @KafkaListener(topics = "product.stats-updated", groupId = "${spring.application.name}-group")
    public void syncStats(String eventJson) {
        try {
            ProductStatsBulkUpdatedEvent event = objectMapper.readValue(eventJson, ProductStatsBulkUpdatedEvent.class);
            syncSearchProductStatsUseCase.execute(event.getStats());
        } catch (Exception e) {
            log.error("Failed to process product.stats-updated event", e);
        }
    }

    /**
     * 1. 결제 완료 -> 판매 확정 처리 위임
     */
    @KafkaListener(topics = "order.product-sale-confirmed", groupId = "${spring.application.name}-group")
    public void handleOrderConfirmed(String eventJson) {
        try {
            OrderProductSaleConfirmedEvent event = objectMapper.readValue(eventJson, OrderProductSaleConfirmedEvent.class);
            log.info("Trigger Confirm Sale: orderUuid={}", event.orderUuid());

            confirmProductSaleUseCase.execute(event.orderUuid(), event.productUuids());
        } catch (Exception e) {
            log.error("Failed to process order.product-sale-confirmed event", e);
        }
    }

    /**
     * 2. 결제 실패/취소 -> 예약 해제 처리 위임
     */
    @KafkaListener(topics = "order.product-sale-released", groupId = "${spring.application.name}-group")
    public void handleOrderReleased(String eventJson) {
        try {
            OrderProductSaleReleasedEvent event = objectMapper.readValue(eventJson, OrderProductSaleReleasedEvent.class);
            log.info("Trigger Release Reservation: orderUuid={}", event.orderUuid());

            releaseProductReservationUseCase.execute(event.orderUuid(), event.productUuids());
        } catch (Exception e) {
            log.error("Failed to process order.product-sale-released event", e);
        }
    }
}
