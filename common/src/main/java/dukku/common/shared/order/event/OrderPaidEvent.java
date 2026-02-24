package dukku.common.shared.order.event;

import java.util.List;
import java.util.UUID;

import dukku.common.global.event.DomainEvent;

public record OrderPaidEvent(
        UUID orderUuid,
        UUID userUuid,
        List<PaidItem> items
) implements DomainEvent {

    public record PaidItem(UUID productUuid, String productName, int productPrice) {}

    @Override
    public String getTopic() {
        return "order.paid";
    }

    @Override
    public String getKey() {
        return orderUuid.toString();
    }
}
