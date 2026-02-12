package dukku.product.global.event;

import dukku.common.global.event.DomainEvent;

public record ProductUpdatedEvent(Integer productId, boolean isCategoryChanged) implements DomainEvent {
    @Override
    public String getTopic() {
        return "product.updated";
    }

    @Override
    public String getKey() {
        return productId.toString();
    }
}
