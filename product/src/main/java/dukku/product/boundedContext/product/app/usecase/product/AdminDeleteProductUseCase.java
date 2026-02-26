package dukku.product.boundedContext.product.app.usecase.product;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.product.dto.product.ProductPayload;
import dukku.common.shared.product.event.ProductSyncEvent;
import dukku.common.shared.product.type.ProductEventType;
import dukku.product.boundedContext.product.app.support.ProductSupport;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.global.event.ProductDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static dukku.product.boundedContext.product.entity.Product.toProductPayload;

@Component
@RequiredArgsConstructor
public class AdminDeleteProductUseCase {
    private static final ProductEventType PRODUCT_EVENT_TYPE = ProductEventType.DELETED;

    private final ProductSupport productSupport;
    private final EventPublisher eventPublisher;

    @Transactional
    public void execute(UUID productUuid) {
        Product product = productSupport.findByUuid(productUuid);

        product.delete();

        eventPublisher.publish(new ProductDeletedEvent(product.getId()));

        ProductPayload payload = toProductPayload(product, PRODUCT_EVENT_TYPE);
        eventPublisher.publish(new ProductSyncEvent(payload));
    }
}
