package dukku.common.shared.order.event;

import dukku.common.global.event.DomainEvent;
import java.util.List;
import java.util.UUID;

public record OrderProductSaleBlockedEvent(UUID orderUuid, List<UUID> productUuids) implements DomainEvent {
    @Override
    public String getTopic() {
        return "order.product.sale-blocked";
    }

    @Override
    public String getKey() {
        return orderUuid.toString();
    }
}
