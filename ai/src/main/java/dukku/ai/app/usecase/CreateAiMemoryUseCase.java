package dukku.ai.app.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dukku.ai.entity.AiMemory;
import dukku.ai.entity.enums.MemorySubType;
import dukku.ai.entity.enums.MemoryType;
import dukku.ai.out.AiMemoryRepository;

@Service
public class CreateAiMemoryUseCase {

    private final AiMemoryRepository aiMemoryRepository;

    public CreateAiMemoryUseCase(AiMemoryRepository aiMemoryRepository) {
        this.aiMemoryRepository = aiMemoryRepository;
    }

    @Transactional
    public AiMemory create(Long userId, MemoryType memoryType, MemorySubType subType,
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
