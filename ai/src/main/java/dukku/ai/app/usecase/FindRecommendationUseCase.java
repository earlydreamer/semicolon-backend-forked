package dukku.ai.app.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dukku.ai.entity.AiMemory;
import dukku.ai.global.policy.AiSimilarityPolicy;
import dukku.ai.out.AiMemoryRepository;
import dukku.common.shared.ai.type.MemoryType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindRecommendationUseCase {

    private final AiMemoryRepository aiMemoryRepository;

    public List<AiMemory> findByUserUuid(UUID userUuid) {
        return aiMemoryRepository.findTopByUserIdAndMemoryType(
                userUuid,
                MemoryType.RECOMMENDATION.name(),
                AiSimilarityPolicy.RECOMMENDATION_TOP_K
        );
    }
}
