package dukku.common.shared.ai.out;

import dukku.common.shared.ai.dto.AiMemoryResponse;
import dukku.common.shared.ai.dto.ChatRequest;
import dukku.common.shared.ai.dto.CreateAiMemoryRequest;
import dukku.common.shared.ai.dto.UpdateAiMemoryRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class AiApiClient {

    private final RestClient restClient;

    public AiApiClient(@Value("${custom.client.ai.url:${custom.global.internalBackUrl:http://localhost:8080}}") String internalBackUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/ai")
                .build();
    }

    public String chat(ChatRequest request) {
        return restClient.post()
                .uri("/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);
    }

    public List<AiMemoryResponse> findAllMemories() {
        return restClient.get()
                .uri("/ai-memories")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public AiMemoryResponse findMemoryById(Integer aiMemoryId) {
        return restClient.get()
                .uri("/ai-memories/{id}", aiMemoryId)
                .retrieve()
                .body(AiMemoryResponse.class);
    }

    public AiMemoryResponse createMemory(CreateAiMemoryRequest request) {
        return restClient.post()
                .uri("/ai-memories")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiMemoryResponse.class);
    }

    public AiMemoryResponse updateMemory(Integer aiMemoryId, UpdateAiMemoryRequest request) {
        return restClient.patch()
                .uri("/ai-memories/{id}", aiMemoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiMemoryResponse.class);
    }

    public void deleteMemory(Integer aiMemoryId) {
        restClient.delete()
                .uri("/ai-memories/{id}", aiMemoryId)
                .retrieve()
                .toBodilessEntity();
    }
}
