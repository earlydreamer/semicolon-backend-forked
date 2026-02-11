package dukku.common.shared.user.dto;

import dukku.common.shared.user.type.Role;
import dukku.common.shared.user.type.UserStatus;

import java.util.UUID;

public record UserDto(
        UUID userUuid,
        String email,
        String nickname,
        Role role,
        UserStatus status
) {
}