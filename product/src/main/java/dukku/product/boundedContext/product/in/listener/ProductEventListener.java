package dukku.product.boundedContext.product.in.listener;

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
import org.springframework.transaction.annotation.Transactional;

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

    // Kafka DTO 역직렬화 전략과 맞추기 위해 String 수신 대신 DTO 시그니처를 사용한다.
    // 1. 생성 동기화
    @Transactional(readOnly = true)
    @KafkaListener(topics = "product.created", groupId = "${spring.application.name}-group")
    public void syncCreate(ProductCreatedEvent event) {
        try {
            log.info("product.created 이벤트 수신: productId={}", event.productId());
            Product product = productRepository.findByIdWithImagesAndCategory(event.productId())
                    .orElseThrow();
            productRepository.preloadProductTagsByProductId(event.productId());
            product.getTagNames();
            saveToElasticSearchUseCase.execute(product, true);
            log.info("product.created 이벤트 처리 완료: productId={}", event.productId());
        } catch (Exception e) {
            log.error("product.created 이벤트 처리 실패: productId={}", event.productId(), e);
            throw e;
        }
    }

    // 2. 수정 동기화
    @Transactional(readOnly = true)
    @KafkaListener(topics = "product.updated", groupId = "${spring.application.name}-group")
    public void syncUpdate(ProductUpdatedEvent event) {
        try {
            log.info("product.updated 이벤트 수신: productId={}, categoryChanged={}",
                    event.productId(), event.isCategoryChanged());
            Product product = productRepository.findByIdWithImagesAndCategory(event.productId())
                    .orElseThrow();
            productRepository.preloadProductTagsByProductId(event.productId());
            product.getTagNames();
            saveToElasticSearchUseCase.execute(product, event.isCategoryChanged());
            log.info("product.updated 이벤트 처리 완료: productId={}", event.productId());
        } catch (Exception e) {
            log.error("product.updated 이벤트 처리 실패: productId={}", event.productId(), e);
            throw e;
        }
    }

    // 3. 삭제 동기화
    @KafkaListener(topics = "product.deleted", groupId = "${spring.application.name}-group")
    public void syncDelete(ProductDeletedEvent event) {
        try {
            log.info("product.deleted 이벤트 수신: productId={}", event.productId());
            productSyncFacade.syncProductToElasticsearch(event.productId().longValue());
            log.info("product.deleted 이벤트 처리 완료: productId={}", event.productId());
        } catch (Exception e) {
            log.error("product.deleted 이벤트 처리 실패: productId={}", event.productId(), e);
            throw e;
        }
    }

    // 4. 통계 동기화 (배치 작업 후 실행)
    @KafkaListener(topics = "product.stats-updated", groupId = "${spring.application.name}-group")
    public void syncStats(ProductStatsBulkUpdatedEvent event) {
        try {
            int size = event.getStats() == null ? 0 : event.getStats().size();
            log.info("product.stats-updated 이벤트 수신: count={}", size);
            syncSearchProductStatsUseCase.execute(event.getStats());
            log.info("product.stats-updated 이벤트 처리 완료: count={}", size);
        } catch (Exception e) {
            log.error("product.stats-updated 이벤트 처리 실패", e);
            throw e;
        }
    }

    /**
     * 1. 결제 완료 -> 판매 확정 처리 위임
     */
    @KafkaListener(topics = "order.product-sale-confirmed", groupId = "${spring.application.name}-group")
    public void handleOrderConfirmed(OrderProductSaleConfirmedEvent event) {
        try {
            log.info("order.product-sale-confirmed 이벤트 수신: orderUuid={}, productCount={}",
                    event.orderUuid(), event.productUuids() == null ? 0 : event.productUuids().size());
            confirmProductSaleUseCase.execute(event.orderUuid(), event.productUuids());
            log.info("order.product-sale-confirmed 이벤트 처리 완료: orderUuid={}", event.orderUuid());
        } catch (Exception e) {
            log.error("order.product-sale-confirmed 이벤트 처리 실패: orderUuid={}", event.orderUuid(), e);
            throw e;
        }
    }

    /**
     * 2. 결제 실패/취소 -> 예약 해제 처리 위임
     */
    @KafkaListener(topics = "order.product-sale-released", groupId = "${spring.application.name}-group")
    public void handleOrderReleased(OrderProductSaleReleasedEvent event) {
        try {
            log.info("order.product-sale-released 이벤트 수신: orderUuid={}, productCount={}",
                    event.orderUuid(), event.productUuids() == null ? 0 : event.productUuids().size());
            releaseProductReservationUseCase.execute(event.orderUuid(), event.productUuids());
            log.info("order.product-sale-released 이벤트 처리 완료: orderUuid={}", event.orderUuid());
        } catch (Exception e) {
            log.error("order.product-sale-released 이벤트 처리 실패: orderUuid={}", event.orderUuid(), e);
            throw e;
        }
    }
}
