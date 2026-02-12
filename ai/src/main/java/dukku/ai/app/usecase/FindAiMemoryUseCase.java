package dukku.ai.app.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dukku.ai.entity.AiMemory;
import dukku.ai.out.AiMemoryRepository;

@Service
@Transactional(readOnly = true)
public class FindAiMemoryUseCase {

    private final AiMemoryRepository aiMemoryRepository;

    public FindAiMemoryUseCase(AiMemoryRepository aiMemoryRepository) {
        this.aiMemoryRepository = aiMemoryRepository;
    }

    public List<AiMemory> findAll() {
        return aiMemoryRepository.findAll();
    }

    public AiMemory findById(Long id) {
        return aiMemoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AI 메모리를 찾을 수 없습니다. id=" + id));
    }
}
