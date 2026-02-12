package dukku.ai.in.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import dukku.ai.app.usecase.CreateAiMemoryUseCase;
import dukku.ai.app.usecase.DeleteAiMemoryUseCase;
import dukku.ai.app.usecase.FindAiMemoryUseCase;
import dukku.ai.app.usecase.UpdateAiMemoryUseCase;
import dukku.ai.entity.AiMemory;
import dukku.ai.entity.enums.MemorySubType;
import dukku.ai.entity.enums.MemoryType;

@Component
public class AiMemoryFacade {

    private final FindAiMemoryUseCase findAiMemoryUseCase;
    private final CreateAiMemoryUseCase createAiMemoryUseCase;
    private final UpdateAiMemoryUseCase updateAiMemoryUseCase;
    private final DeleteAiMemoryUseCase deleteAiMemoryUseCase;

    public AiMemoryFacade(FindAiMemoryUseCase findAiMemoryUseCase,
                          CreateAiMemoryUseCase createAiMemoryUseCase,
                          UpdateAiMemoryUseCase updateAiMemoryUseCase,
                          DeleteAiMemoryUseCase deleteAiMemoryUseCase) {
        this.findAiMemoryUseCase = findAiMemoryUseCase;
        this.createAiMemoryUseCase = createAiMemoryUseCase;
        this.updateAiMemoryUseCase = updateAiMemoryUseCase;
        this.deleteAiMemoryUseCase = deleteAiMemoryUseCase;
    }

    public List<AiMemoryResponse> findAll() {
        return findAiMemoryUseCase.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public AiMemoryResponse findById(Long id) {
        return toResponse(findAiMemoryUseCase.findById(id));
    }

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

    public AiMemoryResponse update(Long id, UpdateAiMemoryRequest request) {
        AiMemory memory = updateAiMemoryUseCase.update(
                id,
                request.importanceScore(),
                request.confidenceScore()
        );
        return toResponse(memory);
    }

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

    public record CreateAiMemoryRequest(
            Long userId,
            MemoryType memoryType,
            MemorySubType subType,
            String content,
            Double importanceScore,
            Double confidenceScore
    ) {
    }

    public record UpdateAiMemoryRequest(
            Double importanceScore,
            Double confidenceScore
    ) {
    }

    public record AiMemoryResponse(
            Long id,
            Long userId,
            MemoryType memoryType,
            MemorySubType subType,
            String content,
            Double importanceScore,
            Double confidenceScore,
            Integer accessCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
