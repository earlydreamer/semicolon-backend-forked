package dukku.common.shared.product.event;

import dukku.common.global.event.DomainEvent;
import dukku.common.shared.product.dto.cart.CartEventPayload;
import dukku.common.shared.product.type.CartEventType;

public record CartSyncEvent(CartEventType eventType, CartEventPayload payload) implements DomainEvent {
    @Override
    public String getTopic() {
        return "cart-events";
    }

    @Override
    public String getKey() {
        return payload.getUserUuid();
    }
}
