package dukku.semicolon.boundedContext.product.in;

import dukku.common.shared.order.event.OrderProductSaleConfirmedEvent;
import dukku.common.shared.order.event.OrderProductSaleReleasedEvent;
import dukku.semicolon.boundedContext.product.app.cqrs.ProductSyncFacade;
import dukku.semicolon.boundedContext.product.app.cqrs.SaveToElasticSearchUseCase;
import dukku.semicolon.boundedContext.product.app.cqrs.SyncProductSearchStatsUseCase;
import dukku.semicolon.boundedContext.product.app.usecase.product.ConfirmProductSaleUseCase;
import dukku.semicolon.boundedContext.product.app.usecase.product.ReleaseProductReservationUseCase;
import dukku.semicolon.shared.product.event.ProductCreatedEvent;
import dukku.semicolon.shared.product.event.ProductDeletedEvent;
import dukku.semicolon.shared.product.event.ProductStatsBulkUpdatedEvent;
import dukku.semicolon.shared.product.event.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductEventListener {
    private final ProductSyncFacade productSyncFacade;
    private final SaveToElasticSearchUseCase saveToElasticSearchUseCase;
    private final SyncProductSearchStatsUseCase  syncProductSearchStatsUseCase;
    private final ConfirmProductSaleUseCase confirmProductSaleUseCase;
    private final ReleaseProductReservationUseCase  releaseProductReservationUseCase;

    // 1. 생성 동기화
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void syncCreate(ProductCreatedEvent event) {
        saveToElasticSearchUseCase.execute(event.product(), true);
    }

    // 2. 수정 동기화
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void syncUpdate(ProductUpdatedEvent event) {
        log.info("Sync Update Product: {}", event.product().getId());

        saveToElasticSearchUseCase.execute(event.product(), event.isCategoryChanged());
    }

    // 3. 삭제 동기화
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void syncDelete(ProductDeletedEvent event) {
        productSyncFacade.syncProductToElasticsearch(event.productId());
    }

    // 4. 통계 동기화 (배치 작업 후 실행)
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void syncStats(ProductStatsBulkUpdatedEvent event) {
        syncProductSearchStatsUseCase.execute(event.getStats());
    }

    /**
     * 1. 결제 완료 -> 판매 확정 처리 위임
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderConfirmed(OrderProductSaleConfirmedEvent event) {
        log.info("Trigger Confirm Sale: orderUuid={}", event.orderUuid());

        confirmProductSaleUseCase.execute(event.orderUuid(), event.productUuids());
    }

    /**
     * 2. 결제 실패/취소 -> 예약 해제 처리 위임
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderReleased(OrderProductSaleReleasedEvent event) {
        log.info("Trigger Release Reservation: orderUuid={}", event.orderUuid());

        releaseProductReservationUseCase.execute(event.orderUuid(), event.productUuids());
    }
}
