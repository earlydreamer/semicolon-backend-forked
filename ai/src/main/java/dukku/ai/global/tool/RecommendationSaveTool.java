package dukku.ai.global.tool;

import java.util.List;
import java.util.UUID;

import dukku.ai.entity.AiUserMemory;
import dukku.ai.out.AiUserMemoryRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import dukku.ai.app.service.GeminiEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationSaveTool {

    private final AiUserMemoryRepository aiUserMemoryRepository;
    private final GeminiEmbeddingService embeddingService;

    @Tool(description = "생성된 추천 결과를 저장합니다")
    public String saveRecommendation(
            ToolContext context,
            @ToolParam(description = "추천 상품 목록") List<String> products) {
        String userId = context.getContext().get("userId").toString();
        log.info("[Tool 호출] 추천 결과 저장: userId={}, productCount={}", userId, products.size());
        try {
            UUID userUuid = UUID.fromString(userId);
            String content = "추천 상품: " + String.join(", ", products);
            float[] embedding = embeddingService.embedDocument(content);

            AiUserMemory memory = AiUserMemory.builder()
                    .userUuid(userUuid)
                    .memoryType(MemoryType.RECOMMENDATION)
                    .subType(MemorySubType.SHOPPING)
                    .content(content)
                    .embedding(embedding)
                    .embeddingProfile(embeddingService.profile())
                    .importanceScore(0.7)
                    .build();

            aiUserMemoryRepository.save(memory);

            log.info("[RecommendationSaveTool] {}건 추천 결과 저장 완료, contentLength={}",
                    products.size(), content.length());
            return "사용자 %s의 추천 결과 %d건 저장 완료".formatted(userId, products.size());
        } catch (Exception e) {
            log.warn("[RecommendationSaveTool] 추천 결과 저장 실패: userId={}, errorType={}",
                    userId, e.getClass().getSimpleName());
            return "추천 결과 저장에 실패했습니다.";
        }
    }
}
