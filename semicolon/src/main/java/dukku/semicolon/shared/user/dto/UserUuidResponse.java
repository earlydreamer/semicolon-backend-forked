package dukku.semicolon.shared.user.dto;

import dukku.semicolon.boundedContext.user.entity.type.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUuidResponse {
    private UUID userUuid;
    private String email;
    private Role role;
}
