package dukku.user.boundedContext.user.in.dto;

import dukku.user.boundedContext.user.type.UserSanctionReasonCode;
import dukku.user.boundedContext.user.type.UserSanctionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class AdminUserSanctionCreateRequest {
    @NotNull
    private UserSanctionType sanctionType;

    @NotNull
    private UserSanctionReasonCode reasonCode;

    @NotBlank
    private String memo;

    private String evidenceUrl;

    private LocalDateTime startAt;

    private LocalDateTime endAt;
}
