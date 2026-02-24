package dukku.ai.app.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import dukku.ai.entity.AiUserMemory;
import dukku.ai.out.AiUserMemoryRepository;
import dukku.common.shared.ai.exception.AiMemoryNotFoundException;

@Service
public class FindAiMemoryUseCase {

    private final AiUserMemoryRepository aiUserMemoryRepository;

    public FindAiMemoryUseCase(AiUserMemoryRepository aiUserMemoryRepository) {
        this.aiUserMemoryRepository = aiUserMemoryRepository;
    }

    public List<AiUserMemory> findAll() {
        return aiUserMemoryRepository.findAll();
    }

    public AiUserMemory findById(Integer aiMemoryId) {
        return aiUserMemoryRepository.findById(aiMemoryId)
                .orElseThrow(() -> new AiMemoryNotFoundException(aiMemoryId));
    }
}
