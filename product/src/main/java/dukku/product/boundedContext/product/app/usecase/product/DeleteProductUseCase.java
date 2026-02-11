package dukku.product.boundedContext.product.app.usecase.product;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.product.boundedContext.product.app.support.ProductSupport;
import dukku.product.boundedContext.product.entity.Product;
import dukku.common.shared.product.event.ProductDeletedEvent;
import dukku.common.shared.product.exception.ProductUnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeleteProductUseCase {
    private final ProductSupport productSupport;
    private final EventPublisher eventPublisher;

    @Transactional
    public void execute(UUID productUuid, UUID sellerUuid) {
        Product product = productSupport.getProduct(productUuid, sellerUuid);

        if (!product.getSellerUuid().equals(sellerUuid)) {
            throw new ProductUnauthorizedException();
        }

        product.delete();

        eventPublisher.publish(new ProductDeletedEvent(product.getId()));
    }
}