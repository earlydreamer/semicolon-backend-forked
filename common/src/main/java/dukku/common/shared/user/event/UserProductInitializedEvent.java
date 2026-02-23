package dukku.common.shared.user.event;

import dukku.common.global.event.DomainEvent;

import java.util.UUID;

public record UserProductInitializedEvent(UUID userUuid, String nickname, String email) implements DomainEvent {
    @Override
    public String getTopic() {
        return "user.product-initialized";
    }

    @Override
    public String getKey() {
        return userUuid.toString();
    }
}