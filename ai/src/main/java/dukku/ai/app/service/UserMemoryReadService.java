package dukku.ai.app.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import dukku.ai.out.AiUserMemoryRepository;
import org.springframework.stereotype.Service;

import dukku.ai.entity.AiUserMemory;
import dukku.ai.global.policy.AiSimilarityPolicy;
import dukku.common.shared.ai.type.MemoryType;

@Service
public class UserMemoryReadService {

    private final AiUserMemoryRepository aiUserMemoryRepository;
    private final GeminiEmbeddingService embeddingService;

    public UserMemoryReadService(AiUserMemoryRepository aiUserMemoryRepository,
                                 GeminiEmbeddingService embeddingService) {
        this.aiUserMemoryRepository = aiUserMemoryRepository;
        this.embeddingService = embeddingService;
    }

    public String retrieveMemoryContext(UUID userUuid, String userMessage) {
        List<AiUserMemory> profileMemories = aiUserMemoryRepository.findTopByUserIdAndMemoryType(
                userUuid, MemoryType.PROFILE.name(), AiSimilarityPolicy.MEMORY_PROFILE_LIMIT);

        float[] queryEmbedding = embeddingService.embedQuery(userMessage);
        String embeddingStr = toVectorString(queryEmbedding);

        List<AiUserMemory> similarMemories = aiUserMemoryRepository.findSimilarMemories(
                userUuid, embeddingStr, embeddingService.profile(), AiSimilarityPolicy.MEMORY_SIMILARITY_THRESHOLD,
                AiSimilarityPolicy.MEMORY_SIMILAR_LIMIT);

        similarMemories.forEach(AiUserMemory::incrementAccessCount);
        if (!similarMemories.isEmpty()) {
            aiUserMemoryRepository.saveAll(similarMemories);
        }

        List<AiUserMemory> allMemories = new ArrayList<>(profileMemories);
        allMemories.addAll(similarMemories);

        if (allMemories.isEmpty()) {
            return "";
        }

        return allMemories.stream()
                .map(m -> "- [%s/%s] %s".formatted(m.getMemoryType(), m.getSubType(), m.getContent()))
                .collect(Collectors.joining("\n", "## 사용자 장기 기억\n", ""));
    }

    private String toVectorString(float[] embedding) {
        return Arrays.toString(embedding);
    }
}
