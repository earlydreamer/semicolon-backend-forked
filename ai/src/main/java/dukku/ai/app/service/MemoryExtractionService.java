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

import dukku.ai.entity.AiUserMemory;
import dukku.ai.global.policy.AiPromptPolicy;
import dukku.ai.global.policy.AiSimilarityPolicy;
import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import dukku.ai.out.AiMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryExtractionService {

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final AiMemoryRepository aiMemoryRepository;
    private final ObjectMapper objectMapper;


    @Async
    public void extractAndStoreMemories(UUID userUuid, String userMessage, String aiResponse) {
        try {
            String prompt = AiPromptPolicy.MEMORY_EXTRACTION_PROMPT.formatted(userMessage, aiResponse);
            String result = chatModel.call(new Prompt(prompt))
                    .getResult()
                    .getOutput()
                    .getText();

            log.info("[기억 추출] userUuid={}, AI 추출 결과: {}", userUuid, result);

            List<MemoryExtraction> extractions = parseExtractions(result);

            if (extractions.isEmpty()) {
                log.info("[기억 추출] userUuid={} — 추출된 기억 없음", userUuid);
                return;
            }

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

        List<AiUserMemory> duplicates = aiMemoryRepository.findDuplicateMemory(
                userUuid, embeddingStr, AiSimilarityPolicy.MEMORY_DUPLICATE_THRESHOLD);

        if (!duplicates.isEmpty()) {
            AiUserMemory existing = duplicates.getFirst();
            existing.updateConfidence(extraction.confidence());
            aiMemoryRepository.save(existing);
            log.info("[기억 추출] 기존 기억 업데이트: id={}, content={}", existing.getId(), existing.getContent());
        } else {
            AiUserMemory newMemory = AiUserMemory.builder()
                    .userUuid(userUuid)
                    .memoryType(MemoryType.valueOf(extraction.memoryType()))
                    .subType(MemorySubType.valueOf(extraction.subType()))
                    .content(extraction.content())
                    .embedding(embedding)
                    .importanceScore(extraction.confidence())
                    .confidenceScore(extraction.confidence())
                    .build();
            aiMemoryRepository.save(newMemory);
            log.info("[기억 추출] 새 기억 저장: type={}/{}, content={}", extraction.memoryType(), extraction.subType(), extraction.content());
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
