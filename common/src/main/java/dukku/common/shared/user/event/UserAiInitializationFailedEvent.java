package dukku.common.shared.user.event;

import dukku.common.global.event.DomainEvent;

import java.util.UUID;

public record UserAiInitializationFailedEvent(UUID userUuid, String email) implements DomainEvent {
    @Override
    public String getTopic() {
        return "user.ai-initialization-failed";
    }

    @Override
    public String getKey() {
        return userUuid.toString();
    }
}