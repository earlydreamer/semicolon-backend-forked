package dukku.auth.boundedContext.auth.infra;

import dukku.auth.boundedContext.auth.exception.UserVerificationFailedException;
import dukku.common.shared.user.dto.SocialUserUpsertRequest;
import dukku.common.shared.user.dto.UserVerificationRequest;
import dukku.common.shared.user.dto.UserVerificationResponse;
import dukku.common.shared.user.type.SocialProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserClient {

    private final RestClient restClient;

    @Value("${service.user.url:http://localhost:8082}")
    private String userServiceUrl;

    public UserVerificationResponse verifyUser(String email, String password) {
        try {
            return restClient.post()
                    .uri(userServiceUrl + "/api/v1/internal/users/verify-password")
                    .body(new UserVerificationRequest(email, password))
                    .retrieve()
                    .body(UserVerificationResponse.class);
        } catch (Exception e) {
            log.error("사용자 인증에 실패했습니다. email={}", email, e);
            throw new UserVerificationFailedException();
        }
    }

    public UserVerificationResponse upsertSocialUser(SocialProvider provider, String email, String nickname) {
        try {
            return restClient.post()
                    .uri(userServiceUrl + "/api/v1/internal/users/social")
                    .body(new SocialUserUpsertRequest(provider, email, nickname))
                    .retrieve()
                    .body(UserVerificationResponse.class);
        } catch (Exception e) {
            log.error("소셜 사용자 생성/조회에 실패했습니다. provider={}, email={}", provider, email, e);
            throw new UserVerificationFailedException();
        }
    }
}
