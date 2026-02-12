package dukku.common.shared.user.event;

import dukku.common.shared.user.dto.UserDto;

import dukku.common.global.event.DomainEvent;

public record UserJoinedEvent(UserDto member) implements DomainEvent {
    @Override
    public String getTopic() {
        return "user.joined";
    }

    @Override
    public String getKey() {
        return member.userUuid().toString();
    }
}
