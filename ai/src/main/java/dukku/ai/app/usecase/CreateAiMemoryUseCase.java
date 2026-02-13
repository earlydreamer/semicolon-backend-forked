package dukku.ai.app.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import dukku.ai.entity.AiMemory;
import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import dukku.ai.out.AiMemoryRepository;

@Service
public class CreateAiMemoryUseCase {

    private final AiMemoryRepository aiMemoryRepository;

    public CreateAiMemoryUseCase(AiMemoryRepository aiMemoryRepository) {
        this.aiMemoryRepository = aiMemoryRepository;
    }

    public AiMemory create(UUID userId, MemoryType memoryType, MemorySubType subType,
                           String content, Double importanceScore, Double confidenceScore) {
        AiMemory memory = AiMemory.builder()
                .userId(userId)
                .memoryType(memoryType)
                .subType(subType)
                .content(content)
                .importanceScore(importanceScore)
                .confidenceScore(confidenceScore)
                .build();
        return aiMemoryRepository.save(memory);
    }
}
