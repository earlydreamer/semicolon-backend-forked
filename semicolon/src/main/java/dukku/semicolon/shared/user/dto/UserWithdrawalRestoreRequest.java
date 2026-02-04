package dukku.semicolon.shared.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserWithdrawalRestoreRequest {

    @NotBlank(message = "New password is required.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*()_+]{8,20}$",
            message = "Password must be 8-20 characters with letters and numbers."
    )
    private String newPassword;
}
