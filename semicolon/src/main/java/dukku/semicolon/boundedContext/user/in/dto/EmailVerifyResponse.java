package dukku.semicolon.boundedContext.user.in.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailVerifyResponse {
    private boolean verified;
    private String email;
}
