package dukku.ai.app.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dukku.ai.entity.AiUserMemory;
import dukku.ai.global.policy.AiPromptPolicy;
import dukku.ai.global.policy.AiSimilarityPolicy;
import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import dukku.ai.out.AiUserMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMemoryWriteService {

    private final ChatModel chatModel;
    private final GeminiEmbeddingService embeddingService;
    private final AiUserMemoryRepository aiUserMemoryRepository;
    private final ObjectMapper objectMapper;


    @Async
    public void extractAndStoreMemories(UUID userUuid, String userMessage, String aiResponse) {
        try {
            String prompt = AiPromptPolicy.MEMORY_EXTRACTION_PROMPT.formatted(userMessage, aiResponse);
            var chatResponse = chatModel.call(new Prompt(prompt));
            String result = chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null
                    ? ""
                    : chatResponse.getResult().getOutput().getText();
            if (result == null) {
                result = "";
            }

            log.info("[기억 추출] userUuid={}, 응답 길이={}", userUuid, result.length());

            List<MemoryExtraction> extractions = parseExtractions(result);

            if (extractions.isEmpty()) {
                log.info("[기억 추출] userUuid={} — 추출된 기억 없음", userUuid);
                return;
            }

            for (MemoryExtraction extraction : extractions) {
                processExtraction(userUuid, extraction);
            }
        } catch (Exception e) {
            log.warn("장기 기억 추출 실패: {}", e.getClass().getSimpleName());
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
            log.warn("기억 추출 JSON 파싱 실패: {}", e.getClass().getSimpleName());
            return List.of();
        }
    }

    private void processExtraction(UUID userUuid, MemoryExtraction extraction) {
        // Duplicate comparison is document-to-document, so generate the document vector once
        // and reuse it for persistence when this extraction is new.
        float[] documentEmbedding = embeddingService.embedDocument(extraction.content());
        String embeddingStr = Arrays.toString(documentEmbedding);

        List<AiUserMemory> duplicates = aiUserMemoryRepository.findDuplicateMemory(
                userUuid, embeddingStr, embeddingService.profile(), AiSimilarityPolicy.MEMORY_DUPLICATE_THRESHOLD);

        if (!duplicates.isEmpty()) {
            AiUserMemory existing = duplicates.getFirst();
            existing.updateImportanceScore(extraction.confidence());
            aiUserMemoryRepository.save(existing);
            log.info("[기억 추출] 기존 기억 점수 갱신: id={}", existing.getId());
        } else {
            AiUserMemory newMemory = AiUserMemory.builder()
                    .userUuid(userUuid)
                    .memoryType(MemoryType.valueOf(extraction.memoryType()))
                    .subType(MemorySubType.valueOf(extraction.subType()))
                    .content(extraction.content())
                    .embedding(documentEmbedding)
                    .embeddingProfile(embeddingService.profile())
                    .importanceScore(extraction.confidence())
                    .build();
            aiUserMemoryRepository.save(newMemory);
            log.info("[기억 추출] 새 기억 저장: type={}/{}, contentLength={}",
                    extraction.memoryType(), extraction.subType(), extraction.content().length());
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
