package dukku.user.boundedContext.user.in.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerifyResultRequest {

    @NotBlank(message = "인증 결과 토큰은 필수입니다.")
    private String resultToken;
}
