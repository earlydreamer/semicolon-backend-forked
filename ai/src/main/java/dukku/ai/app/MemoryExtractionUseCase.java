package dukku.ai.app;

import java.util.Arrays;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dukku.ai.entity.AiMemory;
import dukku.ai.entity.enums.MemorySubType;
import dukku.ai.entity.enums.MemoryType;
import dukku.ai.out.AiMemoryRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MemoryExtractionUseCase {

    private static final double DUPLICATE_THRESHOLD = 0.92;
    private static final String EXTRACTION_PROMPT = """
            다음 대화에서 사용자에 대해 기억할 만한 정보를 추출하세요.
            각 항목을 JSON 배열로 반환하세요. 기억할 정보가 없으면 빈 배열 []을 반환하세요.

            형식:
            [
              {
                "memoryType": "PROFILE|PREFERENCE",
                "subType": "TECH|SHOPPING|GENERAL",
                "content": "기억할 내용",
                "confidence": 0.0~1.0
              }
            ]

            memoryType 기준:
            - PROFILE: 이름, 나이, 직업 등 기본 정보
            - PREFERENCE: 좋아하는 것, 싫어하는 것, 선호도
       
            JSON 배열만 반환하고 다른 텍스트는 포함하지 마세요.

            대화 내용:
            사용자: %s
            AI: %s
            """;

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final AiMemoryRepository aiMemoryRepository;
    private final ObjectMapper objectMapper;

    public MemoryExtractionUseCase(ChatModel chatModel,
                                   EmbeddingModel embeddingModel,
                                   AiMemoryRepository aiMemoryRepository,
                                   ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.aiMemoryRepository = aiMemoryRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    public void extractAndStoreMemories(Long userId, String userMessage, String aiResponse) {
        try {
            String prompt = EXTRACTION_PROMPT.formatted(userMessage, aiResponse);
            String result = chatModel.call(new Prompt(prompt))
                    .getResult()
                    .getOutput()
                    .getText();

            List<MemoryExtraction> extractions = parseExtractions(result);

            for (MemoryExtraction extraction : extractions) {
                processExtraction(userId, extraction);
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

    private void processExtraction(Long userId, MemoryExtraction extraction) {
        float[] embedding = embeddingModel.embed(extraction.content());
        String embeddingStr = Arrays.toString(embedding);

        List<AiMemory> duplicates = aiMemoryRepository.findDuplicateMemory(
                userId, embeddingStr, DUPLICATE_THRESHOLD);

        if (!duplicates.isEmpty()) {
            AiMemory existing = duplicates.getFirst();
            existing.updateConfidence(extraction.confidence());
            aiMemoryRepository.save(existing);
            log.debug("기존 기억 업데이트: id={}", existing.getId());
        } else {
            AiMemory newMemory = AiMemory.builder()
                    .userId(userId)
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
