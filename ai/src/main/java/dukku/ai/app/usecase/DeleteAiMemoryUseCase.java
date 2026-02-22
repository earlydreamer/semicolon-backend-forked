package dukku.ai.app.usecase;

import org.springframework.stereotype.Service;

import dukku.ai.entity.AiMemory;
import dukku.ai.out.AiMemoryRepository;
import dukku.common.shared.ai.exception.AiMemoryNotFoundException;

@Service
public class DeleteAiMemoryUseCase {

    private final AiMemoryRepository aiMemoryRepository;

    public DeleteAiMemoryUseCase(AiMemoryRepository aiMemoryRepository) {
        this.aiMemoryRepository = aiMemoryRepository;
    }

    public void delete(Integer aiMemoryId) {
        AiMemory memory = aiMemoryRepository.findById(aiMemoryId)
                .orElseThrow(() -> new AiMemoryNotFoundException(aiMemoryId));
        aiMemoryRepository.delete(memory);
    }
}
