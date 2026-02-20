package dukku.common.shared.user.event;

import dukku.common.global.event.DomainEvent;

import java.util.UUID;

public record UserProductInitializationFailedEvent(UUID userUuid, String reason) implements DomainEvent {
    @Override
    public String getTopic() {
        return "user.product-initialization-failed";
    }

    @Override
    public String getKey() {
        return userUuid.toString();
    }
}
