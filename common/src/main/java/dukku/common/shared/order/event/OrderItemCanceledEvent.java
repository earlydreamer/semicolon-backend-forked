package dukku.common.shared.order.event;

import dukku.common.global.event.DomainEvent;

import java.util.UUID;

public record OrderItemCanceledEvent(UUID orderItemUuid) implements DomainEvent {
    @Override
    public String getTopic() {
        return "order.item.canceled";
    }

    @Override
    public String getKey() {
        return orderItemUuid.toString();
    }
}
