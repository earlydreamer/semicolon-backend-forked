package dukku.common.shared.auth.out;

import dukku.common.global.auth.InternalServiceTokenResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Component
public class AuthApiClient {

    private final RestClient restClient;

    public AuthApiClient(@Value("${custom.client.auth.url:${custom.global.internalBackUrl:http://localhost:8080}}") String internalBackUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/internal/auth")
                .build();
    }

    public void revokeAllSessions(UUID userUuid) {
        RestClient.RequestHeadersSpec<?> requestSpec = restClient.delete()
                .uri("/users/{userUuid}/sessions", userUuid);

        String internalToken = InternalServiceTokenResolver.resolve();
        if (internalToken != null) {
            requestSpec = requestSpec.header(InternalServiceTokenResolver.HEADER_NAME, internalToken);
        }

        requestSpec.retrieve()
                .toBodilessEntity();
        log.info("Revoked auth sessions for userUuid={}", userUuid);
    }
}
