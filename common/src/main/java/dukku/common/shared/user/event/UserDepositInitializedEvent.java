package dukku.common.shared.user.event;

import dukku.common.global.event.DomainEvent;

import java.util.UUID;

public record UserDepositInitializedEvent(UUID userUuid) implements DomainEvent {
    @Override
    public String getTopic() {
        return "user.deposit-initialized";
    }

    @Override
    public String getKey() {
        return userUuid.toString();
    }
}
