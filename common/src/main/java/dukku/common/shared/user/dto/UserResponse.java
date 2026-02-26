package dukku.common.shared.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import dukku.common.shared.user.type.Role;
import dukku.common.shared.user.type.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@ToString
public class UserResponse {
    private UUID userUuid;
    private String email;
    private String nickname;
    private String intro;
    private Role role;
    private UserStatus status;
    private String statusLabel;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
