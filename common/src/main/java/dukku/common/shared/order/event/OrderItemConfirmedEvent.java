package dukku.common.shared.order.event;

import dukku.common.global.event.DomainEvent;

import java.util.UUID;

public record OrderItemConfirmedEvent(UUID orderItemUuid) implements DomainEvent {
    @Override
    public String getTopic() {
        return "order.item.confirmed";
    }

    @Override
    public String getKey() {
        return orderItemUuid.toString();
    }
}
