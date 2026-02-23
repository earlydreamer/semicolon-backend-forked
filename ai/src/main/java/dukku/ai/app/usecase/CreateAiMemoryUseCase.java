package dukku.ai.app.usecase;

import java.util.UUID;

import dukku.ai.out.AiUserMemoryRepository;
import org.springframework.stereotype.Service;

import dukku.ai.entity.AiUserMemory;
import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;

@Service
public class CreateAiMemoryUseCase {

    private final AiUserMemoryRepository aiUserMemoryRepository;

    public CreateAiMemoryUseCase(AiUserMemoryRepository aiUserMemoryRepository) {
        this.aiUserMemoryRepository = aiUserMemoryRepository;
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
        return aiUserMemoryRepository.save(memory);
    }
}
