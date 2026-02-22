package dukku.common.shared.user.dto;

import dukku.common.shared.user.type.SocialProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SocialUserUpsertRequest {
    @NotNull
    private SocialProvider provider;

    @NotBlank
    @Email
    private String email;

    private String nickname;
}
