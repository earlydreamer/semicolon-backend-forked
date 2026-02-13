package dukku.ai.app.usecase;

import org.springframework.stereotype.Service;

import dukku.ai.entity.AiMemory;
import dukku.ai.out.AiMemoryRepository;
import dukku.common.shared.ai.exception.AiMemoryNotFoundException;

@Service
public class UpdateAiMemoryUseCase {

    private final AiMemoryRepository aiMemoryRepository;

    public UpdateAiMemoryUseCase(AiMemoryRepository aiMemoryRepository) {
        this.aiMemoryRepository = aiMemoryRepository;
    }

    public AiMemory update(Integer id, Double importanceScore, Double confidenceScore) {
        AiMemory memory = aiMemoryRepository.findById(id)
                .orElseThrow(() -> new AiMemoryNotFoundException(id));

        if (importanceScore != null) {
            memory.updateImportanceScore(importanceScore);
        }
        if (confidenceScore != null) {
            memory.updateConfidence(confidenceScore);
        }
        return memory;
    }
}
