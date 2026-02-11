package dukku.common.shared.order.event;

import java.util.List;
import java.util.UUID;

import dukku.common.global.event.DomainEvent;

public record OrderProductSaleReleasedEvent(UUID orderUuid, List<UUID> productUuids) implements DomainEvent {
    @Override
    public String getTopic() {
        return "order.product-sale-released";
    }

    @Override
    public String getKey() {
        return orderUuid.toString();
    }
}
