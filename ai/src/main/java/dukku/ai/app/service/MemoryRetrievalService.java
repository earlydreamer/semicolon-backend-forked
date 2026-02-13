package dukku.ai.app.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import dukku.ai.entity.AiMemory;
import dukku.ai.entity.enums.MemoryType;
import dukku.ai.out.AiMemoryRepository;

@Service
public class MemoryRetrievalService {

    private static final double SIMILARITY_THRESHOLD = 0.3;
    private static final int PROFILE_LIMIT = 3;
    private static final int SIMILAR_LIMIT = 5;

    private final AiMemoryRepository aiMemoryRepository;
    private final EmbeddingModel embeddingModel;

    public MemoryRetrievalService(AiMemoryRepository aiMemoryRepository,
                                  EmbeddingModel embeddingModel) {
        this.aiMemoryRepository = aiMemoryRepository;
        this.embeddingModel = embeddingModel;
    }

    public String retrieveMemoryContext(Long userId, String userMessage) {
        List<AiMemory> profileMemories = aiMemoryRepository.findTopByUserIdAndMemoryType(
                userId, MemoryType.PROFILE.name(), PROFILE_LIMIT);

        float[] queryEmbedding = embeddingModel.embed(userMessage);
        String embeddingStr = toVectorString(queryEmbedding);

        List<AiMemory> similarMemories = aiMemoryRepository.findSimilarMemories(
                userId, embeddingStr, SIMILARITY_THRESHOLD, SIMILAR_LIMIT);

        similarMemories.forEach(AiMemory::incrementAccessCount);
        if (!similarMemories.isEmpty()) {
            aiMemoryRepository.saveAll(similarMemories);
        }

        List<AiMemory> allMemories = new ArrayList<>(profileMemories);
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
