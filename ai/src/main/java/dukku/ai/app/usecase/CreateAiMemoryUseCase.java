package dukku.ai.app.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import dukku.ai.entity.AiUserMemory;
import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import dukku.ai.out.AiMemoryRepository;

@Service
public class CreateAiMemoryUseCase {

    private final AiMemoryRepository aiMemoryRepository;

    public CreateAiMemoryUseCase(AiMemoryRepository aiMemoryRepository) {
        this.aiMemoryRepository = aiMemoryRepository;
    }

    public AiUserMemory create(UUID userUuid, MemoryType memoryType, MemorySubType subType,
                               String content, Double importanceScore) {
        AiUserMemory memory = AiUserMemory.builder()
                .userUuid(userUuid)
                .memoryType(memoryType)
                .subType(subType)
                .content(content)
                .importanceScore(importanceScore)
                .build();
        return aiMemoryRepository.save(memory);
    }
}
