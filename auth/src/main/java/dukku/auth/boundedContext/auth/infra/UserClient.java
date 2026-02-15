package dukku.auth.boundedContext.auth.infra;

import dukku.auth.boundedContext.auth.exception.UserVerificationFailedException;
import dukku.common.shared.user.dto.UserVerificationRequest;
import dukku.common.shared.user.dto.UserVerificationResponse;
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
            log.error("User verification failed for email: {}", email, e);
            throw new UserVerificationFailedException();
        }
    }
}