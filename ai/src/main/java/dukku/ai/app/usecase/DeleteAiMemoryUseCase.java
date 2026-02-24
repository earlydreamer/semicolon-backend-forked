package dukku.ai.app.usecase;

import dukku.ai.out.AiUserMemoryRepository;
import org.springframework.stereotype.Service;

import dukku.ai.entity.AiUserMemory;
import dukku.common.shared.ai.exception.AiMemoryNotFoundException;

@Service
public class DeleteAiMemoryUseCase {

    private final AiUserMemoryRepository aiUserMemoryRepository;

    public DeleteAiMemoryUseCase(AiUserMemoryRepository aiUserMemoryRepository) {
        this.aiUserMemoryRepository = aiUserMemoryRepository;
    }

    public void delete(Integer aiMemoryId) {
        AiUserMemory memory = aiUserMemoryRepository.findById(aiMemoryId)
                .orElseThrow(() -> new AiMemoryNotFoundException(aiMemoryId));
        aiUserMemoryRepository.delete(memory);
    }
}
