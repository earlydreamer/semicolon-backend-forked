package dukku.ai.app.usecase;

import org.springframework.stereotype.Service;

import dukku.ai.entity.AiMemory;
import dukku.ai.out.AiMemoryRepository;

@Service
public class UpdateAiMemoryUseCase {

    private final AiMemoryRepository aiMemoryRepository;

    public UpdateAiMemoryUseCase(AiMemoryRepository aiMemoryRepository) {
        this.aiMemoryRepository = aiMemoryRepository;
    }

    public AiMemory update(Long id, Double importanceScore, Double confidenceScore) {
        AiMemory memory = aiMemoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AI 메모리를 찾을 수 없습니다. id=" + id));

        if (importanceScore != null) {
            memory.updateImportanceScore(importanceScore);
        }
        if (confidenceScore != null) {
            memory.updateConfidence(confidenceScore);
        }
        return memory;
    }
}
