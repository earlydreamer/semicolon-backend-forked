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

    public void delete(Integer id) {
        AiMemory memory = aiMemoryRepository.findById(id)
                .orElseThrow(() -> new AiMemoryNotFoundException(id));
        aiMemoryRepository.delete(memory);
    }
}
