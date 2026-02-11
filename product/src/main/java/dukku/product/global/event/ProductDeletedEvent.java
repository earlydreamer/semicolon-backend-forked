package dukku.product.global.event;

import dukku.common.global.event.DomainEvent;

public record ProductDeletedEvent(Integer productId) implements DomainEvent {
    @Override
    public String getTopic() {
        return "product.deleted";
    }

    @Override
    public String getKey() {
        return productId.toString();
    }
}
