package dukku.ai.app.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dukku.ai.entity.AiMemory;
import dukku.ai.global.policy.AiPromptPolicy;
import dukku.ai.global.policy.AiSimilarityPolicy;
import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import dukku.ai.out.AiMemoryRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MemoryExtractionService {

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final AiMemoryRepository aiMemoryRepository;
    private final ObjectMapper objectMapper;

    public MemoryExtractionService(ChatModel chatModel,
                                   EmbeddingModel embeddingModel,
                                   AiMemoryRepository aiMemoryRepository,
                                   ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.aiMemoryRepository = aiMemoryRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    public void extractAndStoreMemories(UUID userUuid, String userMessage, String aiResponse) {
        try {
            String prompt = AiPromptPolicy.MEMORY_EXTRACTION_PROMPT.formatted(userMessage, aiResponse);
            String result = chatModel.call(new Prompt(prompt))
                    .getResult()
                    .getOutput()
                    .getText();

            List<MemoryExtraction> extractions = parseExtractions(result);

            for (MemoryExtraction extraction : extractions) {
                processExtraction(userUuid, extraction);
            }
        } catch (Exception e) {
            log.warn("장기 기억 추출 실패: {}", e.getMessage(), e);
        }
    }

    private List<MemoryExtraction> parseExtractions(String result) {
        try {
            String json = result.strip();
            if (json.startsWith("```")) {
                json = json.replaceAll("```(?:json)?\\s*", "").replaceAll("```\\s*$", "").strip();
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("기억 추출 JSON 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private void processExtraction(UUID userUuid, MemoryExtraction extraction) {
        float[] embedding = embeddingModel.embed(extraction.content());
        String embeddingStr = Arrays.toString(embedding);

        List<AiMemory> duplicates = aiMemoryRepository.findDuplicateMemory(
                userUuid, embeddingStr, AiSimilarityPolicy.MEMORY_DUPLICATE_THRESHOLD);

        if (!duplicates.isEmpty()) {
            AiMemory existing = duplicates.getFirst();
            existing.updateConfidence(extraction.confidence());
            aiMemoryRepository.save(existing);
            log.debug("기존 기억 업데이트: id={}", existing.getId());
        } else {
            AiMemory newMemory = AiMemory.builder()
                    .userUuid(userUuid)
                    .memoryType(MemoryType.valueOf(extraction.memoryType()))
                    .subType(MemorySubType.valueOf(extraction.subType()))
                    .content(extraction.content())
                    .embedding(embedding)
                    .importanceScore(extraction.confidence())
                    .confidenceScore(extraction.confidence())
                    .build();
            aiMemoryRepository.save(newMemory);
            log.debug("새 기억 저장: content={}", extraction.content());
        }
    }

    private record MemoryExtraction(
            String memoryType,
            String subType,
            String content,
            Double confidence
    ) {
    }
}
