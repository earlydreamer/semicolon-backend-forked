package dukku.ai.app.usecase;

import java.util.List;
import java.util.UUID;

import dukku.ai.entity.AiUserMemory;
import dukku.ai.out.AiUserMemoryRepository;
import org.springframework.stereotype.Service;

import dukku.ai.global.policy.AiSimilarityPolicy;
import dukku.common.shared.ai.type.MemoryType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindRecommendationUseCase {

    private final AiUserMemoryRepository aiUserMemoryRepository;

    public List<AiUserMemory> findByUserUuid(UUID userUuid) {
        return aiUserMemoryRepository.findTopByUserIdAndMemoryType(
                userUuid,
                MemoryType.RECOMMENDATION.name(),
                AiSimilarityPolicy.RECOMMENDATION_TOP_K
        );
    }
}
