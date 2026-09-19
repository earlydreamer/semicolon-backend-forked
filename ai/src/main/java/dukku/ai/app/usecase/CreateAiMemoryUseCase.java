package dukku.ai.app.usecase;

import java.util.UUID;

import dukku.ai.out.AiUserMemoryRepository;
import dukku.ai.app.service.GeminiEmbeddingService;
import org.springframework.stereotype.Service;

import dukku.ai.entity.AiUserMemory;
import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;

@Service
public class CreateAiMemoryUseCase {

    private final AiUserMemoryRepository aiUserMemoryRepository;
    private final GeminiEmbeddingService embeddingService;

    public CreateAiMemoryUseCase(AiUserMemoryRepository aiUserMemoryRepository,
                                 GeminiEmbeddingService embeddingService) {
        this.aiUserMemoryRepository = aiUserMemoryRepository;
        this.embeddingService = embeddingService;
    }

    public AiUserMemory create(UUID userUuid, MemoryType memoryType, MemorySubType subType,
                               String content, Double importanceScore) {
        float[] embedding = embeddingService.embedDocument(content);
        AiUserMemory memory = AiUserMemory.builder()
                .userUuid(userUuid)
                .memoryType(memoryType)
                .subType(subType)
                .content(content)
                .embedding(embedding)
                .embeddingProfile(embeddingService.profile())
                .importanceScore(importanceScore)
                .build();
        return aiUserMemoryRepository.save(memory);
    }
}
