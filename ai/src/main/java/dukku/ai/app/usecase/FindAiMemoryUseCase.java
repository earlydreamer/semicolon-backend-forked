package dukku.ai.app.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import dukku.ai.entity.AiUserMemory;
import dukku.ai.out.AiMemoryRepository;
import dukku.common.shared.ai.exception.AiMemoryNotFoundException;

@Service
public class FindAiMemoryUseCase {

    private final AiMemoryRepository aiMemoryRepository;

    public FindAiMemoryUseCase(AiMemoryRepository aiMemoryRepository) {
        this.aiMemoryRepository = aiMemoryRepository;
    }

    public List<AiUserMemory> findAll() {
        return aiMemoryRepository.findAll();
    }

    public AiUserMemory findById(Integer aiMemoryId) {
        return aiMemoryRepository.findById(aiMemoryId)
                .orElseThrow(() -> new AiMemoryNotFoundException(aiMemoryId));
    }
}
