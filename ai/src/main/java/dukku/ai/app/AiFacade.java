package dukku.ai.app;

import java.util.List;
import java.util.UUID;

import dukku.ai.app.usecase.*;
import dukku.common.shared.ai.dto.AiMemoryResponse;
import dukku.common.shared.ai.dto.ChatRequest;
import dukku.common.shared.ai.dto.CreateAiMemoryRequest;
import dukku.common.shared.ai.dto.UpdateAiMemoryRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import dukku.ai.entity.AiMemory;

@Component
@Transactional(readOnly = true)
public class AiFacade {

    private final ChatUseCase chatUseCase;
    private final FindAiMemoryUseCase findAiMemoryUseCase;
    private final CreateAiMemoryUseCase createAiMemoryUseCase;
    private final UpdateAiMemoryUseCase updateAiMemoryUseCase;
    private final DeleteAiMemoryUseCase deleteAiMemoryUseCase;

    public AiFacade(ChatUseCase chatUseCase, FindAiMemoryUseCase findAiMemoryUseCase, CreateAiMemoryUseCase createAiMemoryUseCase, UpdateAiMemoryUseCase updateAiMemoryUseCase, DeleteAiMemoryUseCase deleteAiMemoryUseCase) {
        this.chatUseCase = chatUseCase;
        this.findAiMemoryUseCase = findAiMemoryUseCase;
        this.createAiMemoryUseCase = createAiMemoryUseCase;
        this.updateAiMemoryUseCase = updateAiMemoryUseCase;
        this.deleteAiMemoryUseCase = deleteAiMemoryUseCase;
    }

    public Flux<String> chat(ChatRequest request) {
        String conversationId = resolveConversationId(request.conversationId());
        return chatUseCase.chat(conversationId, request.userId(), request.message());
    }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return conversationId;
    }

    public List<AiMemoryResponse> findAll() {
        return findAiMemoryUseCase.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public AiMemoryResponse findById(Long id) {
        return toResponse(findAiMemoryUseCase.findById(id));
    }

    @Transactional
    public AiMemoryResponse create(CreateAiMemoryRequest request) {
        AiMemory memory = createAiMemoryUseCase.create(
                request.userId(),
                request.memoryType(),
                request.subType(),
                request.content(),
                request.importanceScore(),
                request.confidenceScore()
        );
        return toResponse(memory);
    }

    @Transactional
    public AiMemoryResponse update(Long id, UpdateAiMemoryRequest request) {
        AiMemory memory = updateAiMemoryUseCase.update(
                id,
                request.importanceScore(),
                request.confidenceScore()
        );
        return toResponse(memory);
    }

    @Transactional
    public void delete(Long id) {
        deleteAiMemoryUseCase.delete(id);
    }

    private AiMemoryResponse toResponse(AiMemory memory) {
        return new AiMemoryResponse(
                memory.getId(),
                memory.getUserId(),
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
