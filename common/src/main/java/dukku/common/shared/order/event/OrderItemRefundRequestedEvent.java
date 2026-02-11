package dukku.common.shared.order.event;

import dukku.common.global.event.DomainEvent;

import java.util.UUID;

public record OrderItemRefundRequestedEvent(UUID orderItemUuid) implements DomainEvent {
    @Override
    public String getTopic() {
        return "order.item.refund-requested";
    }

    @Override
    public String getKey() {
        return orderItemUuid.toString();
    }
}
