package dukku.common.shared.ai.out;

import dukku.common.global.auth.RequestAuthorizationHeaderResolver;
import dukku.common.shared.ai.dto.AiUserMemoryResponse;
import dukku.common.shared.ai.dto.ChatRequest;
import dukku.common.shared.ai.dto.CreateAiUserMemoryRequest;
import dukku.common.shared.ai.dto.UpdateAiUserMemoryRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

@Component
public class AiApiClient {

    private final RestClient restClient;

    public AiApiClient(@Value("${custom.client.ai.url:${custom.global.internalBackUrl:http://localhost:8080}}") String internalBackUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/ai")
                .requestFactory(factory)
                .build();
    }

    public String chat(ChatRequest request) {
        RestClient.RequestBodySpec requestSpec = restClient.post()
                .uri("/chat")
                .contentType(MediaType.APPLICATION_JSON);

        String authorization = RequestAuthorizationHeaderResolver.resolve();
        if (authorization != null) {
            requestSpec = requestSpec.header("Authorization", authorization);
        }

        return requestSpec.body(request)
                .retrieve()
                .body(String.class);
    }

    public List<AiUserMemoryResponse> findAllMemories() {
        RestClient.RequestHeadersSpec<?> requestSpec = restClient.get()
                .uri("/ai-memories");

        String authorization = RequestAuthorizationHeaderResolver.resolve();
        if (authorization != null) {
            requestSpec = requestSpec.header("Authorization", authorization);
        }

        return requestSpec.retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public AiUserMemoryResponse findMemoryById(Integer aiMemoryId) {
        RestClient.RequestHeadersSpec<?> requestSpec = restClient.get()
                .uri("/ai-memories/{id}", aiMemoryId);

        String authorization = RequestAuthorizationHeaderResolver.resolve();
        if (authorization != null) {
            requestSpec = requestSpec.header("Authorization", authorization);
        }

        return requestSpec.retrieve()
                .body(AiUserMemoryResponse.class);
    }

    public AiUserMemoryResponse createMemory(CreateAiUserMemoryRequest request) {
        RestClient.RequestBodySpec requestSpec = restClient.post()
                .uri("/ai-memories")
                .contentType(MediaType.APPLICATION_JSON);

        String authorization = RequestAuthorizationHeaderResolver.resolve();
        if (authorization != null) {
            requestSpec = requestSpec.header("Authorization", authorization);
        }

        return requestSpec.body(request)
                .retrieve()
                .body(AiUserMemoryResponse.class);
    }

    public AiUserMemoryResponse updateMemory(Integer aiMemoryId, UpdateAiUserMemoryRequest request) {
        RestClient.RequestBodySpec requestSpec = restClient.patch()
                .uri("/ai-memories/{id}", aiMemoryId)
                .contentType(MediaType.APPLICATION_JSON);

        String authorization = RequestAuthorizationHeaderResolver.resolve();
        if (authorization != null) {
            requestSpec = requestSpec.header("Authorization", authorization);
        }

        return requestSpec.body(request)
                .retrieve()
                .body(AiUserMemoryResponse.class);
    }

    public void deleteMemory(Integer aiMemoryId) {
        RestClient.RequestHeadersSpec<?> requestSpec = restClient.delete()
                .uri("/ai-memories/{id}", aiMemoryId);

        String authorization = RequestAuthorizationHeaderResolver.resolve();
        if (authorization != null) {
            requestSpec = requestSpec.header("Authorization", authorization);
        }

        requestSpec.retrieve()
                .toBodilessEntity();
    }
}
