package dukku.ai.app.usecase;

import org.springframework.stereotype.Service;

import dukku.ai.entity.AiMemory;
import dukku.ai.out.AiMemoryRepository;

@Service
public class DeleteAiMemoryUseCase {

    private final AiMemoryRepository aiMemoryRepository;

    public DeleteAiMemoryUseCase(AiMemoryRepository aiMemoryRepository) {
        this.aiMemoryRepository = aiMemoryRepository;
    }

    public void delete(Long id) {
        AiMemory memory = aiMemoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AI 메모리를 찾을 수 없습니다. id=" + id));
        aiMemoryRepository.delete(memory);
    }
}
