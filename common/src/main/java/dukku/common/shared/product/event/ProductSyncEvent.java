package dukku.common.shared.product.event;

import dukku.common.global.event.DomainEvent;
import dukku.common.shared.product.dto.product.ProductPayload;

public record ProductSyncEvent(ProductPayload payload) implements DomainEvent {
    @Override
    public String getTopic() {
        return "product-events";
    }

    @Override
    public String getKey() {
        return payload.productUuid().toString();
    }
}
