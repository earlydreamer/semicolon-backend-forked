package dukku.common.shared.order.event;

import java.util.UUID;

import dukku.common.global.event.DomainEvent;

public record PaymentRollbackRequestEvent(UUID orderUuid, String reason) implements DomainEvent {
    @Override
    public String getTopic() {
        return "payment.rollback";
    }

    @Override
    public String getKey() {
        return orderUuid.toString();
    }
}
