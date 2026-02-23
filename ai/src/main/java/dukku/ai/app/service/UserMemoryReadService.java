package dukku.ai.app.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import dukku.ai.entity.AiUserMemory;
import dukku.ai.global.policy.AiSimilarityPolicy;
import dukku.common.shared.ai.type.MemoryType;
import dukku.ai.out.AiMemoryRepository;

@Service
public class UserMemoryReadService {

    private final AiMemoryRepository aiMemoryRepository;
    private final EmbeddingModel embeddingModel;

    public UserMemoryReadService(AiMemoryRepository aiMemoryRepository,
                                 EmbeddingModel embeddingModel) {
        this.aiMemoryRepository = aiMemoryRepository;
        this.embeddingModel = embeddingModel;
    }

    public String retrieveMemoryContext(UUID userUuid, String userMessage) {
        List<AiUserMemory> profileMemories = aiMemoryRepository.findTopByUserIdAndMemoryType(
                userUuid, MemoryType.PROFILE.name(), AiSimilarityPolicy.MEMORY_PROFILE_LIMIT);

        float[] queryEmbedding = embeddingModel.embed(userMessage);
        String embeddingStr = toVectorString(queryEmbedding);

        List<AiUserMemory> similarMemories = aiMemoryRepository.findSimilarMemories(
                userUuid, embeddingStr, AiSimilarityPolicy.MEMORY_SIMILARITY_THRESHOLD, AiSimilarityPolicy.MEMORY_SIMILAR_LIMIT);

        similarMemories.forEach(AiUserMemory::incrementAccessCount);
        if (!similarMemories.isEmpty()) {
            aiMemoryRepository.saveAll(similarMemories);
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
