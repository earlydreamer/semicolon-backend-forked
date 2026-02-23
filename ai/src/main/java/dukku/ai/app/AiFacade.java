package dukku.ai.app;

import dukku.ai.app.usecase.*;
import dukku.ai.entity.AiUserMemory;
import dukku.common.shared.ai.dto.AiUserMemoryResponse;
import dukku.common.shared.ai.dto.ChatRequest;
import dukku.common.shared.ai.dto.CreateAiUserMemoryRequest;
import dukku.common.shared.ai.dto.UpdateAiUserMemoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiFacade {

    private final ChatUseCase chatUseCase;
    private final FindAiMemoryUseCase findAiMemoryUseCase;
    private final CreateAiMemoryUseCase createAiMemoryUseCase;
    private final UpdateAiMemoryUseCase updateAiMemoryUseCase;
    private final DeleteAiMemoryUseCase deleteAiMemoryUseCase;
    private final FindRecommendationUseCase findRecommendationUseCase;


    public Flux<String> chat(ChatRequest request) {
        String conversationId = resolveConversationId(request.conversationId());
        return chatUseCase.chat(conversationId, request.userUuid(), request.message());
    }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return conversationId;
    }

    public List<AiUserMemoryResponse> findAll() {
        return findAiMemoryUseCase.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public AiUserMemoryResponse findById(Integer aiMemoryId) {
        return toResponse(findAiMemoryUseCase.findById(aiMemoryId));
    }

    @Transactional
    public AiUserMemoryResponse create(CreateAiUserMemoryRequest request) {
        AiUserMemory memory = createAiMemoryUseCase.create(
                request.userUuid(),
                request.memoryType(),
                request.subType(),
                request.content(),
                request.importanceScore(),
                request.confidenceScore()
        );
        return toResponse(memory);
    }

    @Transactional
    public AiUserMemoryResponse update(Integer aiMemoryId, UpdateAiUserMemoryRequest request) {
        AiUserMemory memory = updateAiMemoryUseCase.update(
                aiMemoryId,
                request.importanceScore(),
                request.confidenceScore()
        );
        return toResponse(memory);
    }

    @Transactional
    public void delete(Integer aiMemoryId) {
        deleteAiMemoryUseCase.delete(aiMemoryId);
    }

    @Transactional(readOnly = true)
    public List<AiUserMemoryResponse> findRecommendations(UUID userUuid) {
        return findRecommendationUseCase.findByUserUuid(userUuid).stream()
                .map(this::toResponse)
                .toList();
    }

    private AiUserMemoryResponse toResponse(AiUserMemory memory) {
        return new AiUserMemoryResponse(
                memory.getId(),
                memory.getUserUuid(),
                memory.getMemoryType(),
                memory.getSubType(),
                memory.getContent(),
                memory.getImportanceScore(),
                memory.getConfidenceScore(),
                memory.getAccessCount(),
                memory.getCreatedAt(),
                memory.getUpdatedAt()
        );
    }
}
