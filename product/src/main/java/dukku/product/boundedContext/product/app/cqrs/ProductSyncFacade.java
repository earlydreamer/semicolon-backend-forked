package dukku.product.boundedContext.product.app.cqrs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductSyncFacade {
    private final SyncProductStatsUseCase syncProductStatsUseCase;
    private final DeleteProductSyncUseCase deleteProductSyncUseCase;

    @Transactional
    public void syncAllStats() {
        syncProductStatsUseCase.execute();
    }

    public void syncProductToElasticsearch(Long productId) {
        deleteProductSyncUseCase.deletedProductToElasticsearch(productId);
    }
}
