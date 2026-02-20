package dukku.common.shared.product.event;

import dukku.common.global.event.DomainEvent;
import dukku.common.shared.product.dto.cart.CartPayload;

public record CartSyncEvent(CartPayload payload) implements DomainEvent {
    @Override
    public String getTopic() {
        return "cart-events";
    }

    @Override
    public String getKey() {
        return payload.userUuid().toString();
    }
}
