package dukku.ai.global.tool;

import java.util.List;
import java.util.UUID;

import dukku.ai.entity.AiUserMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import dukku.ai.out.AiMemoryRepository;
import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationSaveTool {

    private final AiMemoryRepository aiMemoryRepository;
    private final EmbeddingModel embeddingModel;

    @Tool(description = "생성된 추천 결과를 저장합니다")
    public String saveRecommendation(
            ToolContext context,
            @ToolParam(description = "추천 상품 목록") List<String> products) {
        String userId = context.getContext().get("userId").toString();
        log.info("[Tool 호출] 추천 결과 저장: userId={}, products={}", userId, products);
        try {
            UUID userUuid = UUID.fromString(userId);
            String content = "추천 상품: " + String.join(", ", products);
            float[] embedding = embeddingModel.embed(content);

            AiUserMemory memory = AiUserMemory.builder()
                    .userUuid(userUuid)
                    .memoryType(MemoryType.RECOMMENDATION)
                    .subType(MemorySubType.SHOPPING)
                    .content(content)
                    .embedding(embedding)
                    .importanceScore(0.7)
                    .confidenceScore(0.8)
                    .build();

            aiMemoryRepository.save(memory);

            log.info("[RecommendationSaveTool] {}건 추천 결과 저장 완료", products.size());
            return "사용자 %s의 추천 결과 %d건 저장 완료".formatted(userId, products.size());
        } catch (Exception e) {
            log.warn("[RecommendationSaveTool] 추천 결과 저장 실패: userId={}, error={}", userId, e.getMessage());
            return "추천 결과 저장에 실패했습니다.";
        }
    }
}
