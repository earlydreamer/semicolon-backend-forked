package dukku.common.shared.user.out;

import dukku.common.global.auth.RequestAuthorizationHeaderResolver;
import dukku.common.shared.user.dto.UserAdminProfileResponse;
import dukku.common.shared.user.dto.UserProfileResponse;
import dukku.common.shared.user.dto.UserUuidResponse;
import dukku.common.shared.user.type.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
public class UserApiClient {
    private final RestClient restClient;
    private final RestClient internalRestClient;

    public UserApiClient(@Value("${custom.client.user.url:${custom.global.internalBackUrl}}") String internalBackUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/users")
                .build();
        this.internalRestClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/internal/users")
                .build();
    }

    public String getRandomSecureTip() {
        return restClient.get()
                .uri("/randomSecureTip")
                .retrieve()
                .body(String.class);
    }

    public UserUuidResponse getUserUuidByRole(Role role) {
        RestClient.RequestHeadersSpec<?> requestSpec = internalRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uuid")
                        .queryParam("role", role)
                        .build());

        String authorization = RequestAuthorizationHeaderResolver.resolve();
        if (authorization != null) {
            requestSpec = requestSpec.header("Authorization", authorization);
        }

        return requestSpec.retrieve()
                .body(UserUuidResponse.class);
    }

    public UserUuidResponse getUserUuidByEmail(String email) {
        RestClient.RequestHeadersSpec<?> requestSpec = internalRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uuid")
                        .queryParam("email", email)
                        .build());

        String authorization = RequestAuthorizationHeaderResolver.resolve();
        if (authorization != null) {
            requestSpec = requestSpec.header("Authorization", authorization);
        }

        return requestSpec.retrieve()
                .body(UserUuidResponse.class);
    }

    public UserProfileResponse getUserProfile(UUID userUuid) {
        RestClient.RequestHeadersSpec<?> requestSpec = internalRestClient.get()
                .uri("/{userUuid}/profile", userUuid);

        String authorization = RequestAuthorizationHeaderResolver.resolve();
        if (authorization != null) {
            requestSpec = requestSpec.header("Authorization", authorization);
        }

        return requestSpec.retrieve()
                .body(UserProfileResponse.class);
    }

    public UserAdminProfileResponse getUserAdminProfile(UUID userUuid) {
        RestClient.RequestHeadersSpec<?> requestSpec = internalRestClient.get()
                .uri("/{userUuid}/admin", userUuid);

        String authorization = RequestAuthorizationHeaderResolver.resolve();
        if (authorization != null) {
            requestSpec = requestSpec.header("Authorization", authorization);
        }

        return requestSpec.retrieve()
                .body(UserAdminProfileResponse.class);
    }
}
