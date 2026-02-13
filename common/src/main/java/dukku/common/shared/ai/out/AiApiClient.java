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

    public AiApiClient(@Value("${custom.global.aiBackUrl}") String aiBackUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(aiBackUrl + "/api")
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

    public AiMemoryResponse findMemoryById(Integer id) {
        return restClient.get()
                .uri("/ai-memories/{id}", id)
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

    public AiMemoryResponse updateMemory(Integer id, UpdateAiMemoryRequest request) {
        return restClient.patch()
                .uri("/ai-memories/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiMemoryResponse.class);
    }

    public void deleteMemory(Integer id) {
        restClient.delete()
                .uri("/ai-memories/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
