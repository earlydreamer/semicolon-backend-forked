package dukku.common.shared.user.event;

import dukku.common.shared.user.dto.UserDto;

public record UserModifiedEvent(UserDto member) {
}
