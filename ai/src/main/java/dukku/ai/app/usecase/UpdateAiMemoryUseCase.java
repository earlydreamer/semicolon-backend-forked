package dukku.ai.app.usecase;

import dukku.ai.out.AiUserMemoryRepository;
import org.springframework.stereotype.Service;

import dukku.ai.entity.AiUserMemory;
import dukku.common.shared.ai.exception.AiMemoryNotFoundException;

@Service
public class UpdateAiMemoryUseCase {

    private final AiUserMemoryRepository aiUserMemoryRepository;

    public UpdateAiMemoryUseCase(AiUserMemoryRepository aiUserMemoryRepository) {
        this.aiUserMemoryRepository = aiUserMemoryRepository;
    }

    public AiUserMemory update(Integer aiMemoryId, Double importanceScore) {
        AiUserMemory memory = aiUserMemoryRepository.findById(aiMemoryId)
                .orElseThrow(() -> new AiMemoryNotFoundException(aiMemoryId));

        if (importanceScore != null) {
            memory.updateImportanceScore(importanceScore);
        }
        return memory;
    }
}
