package dukku.semicolon.boundedContext.product.app.cqrs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductSyncFacade {
    private final ProductStatsUseCase productStatsUseCase;
    private final DeleteProductSyncUseCase deleteProductSyncUseCase;

    @Transactional
    public void syncAllStats() {
        productStatsUseCase.execute();
    }

    public void syncProductToElasticsearch(int productId) {
        deleteProductSyncUseCase.deletedProductToElasticsearch(productId);
    }
}