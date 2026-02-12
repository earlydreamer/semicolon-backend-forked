package dukku.product.global.event;

import dukku.common.global.event.DomainEvent;

public record ProductCreatedEvent(Integer productId) implements DomainEvent {
    @Override
    public String getTopic() {
        return "product.created";
    }

    @Override
    public String getKey() {
        return productId.toString();
    }
}
