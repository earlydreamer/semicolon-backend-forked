package dukku.ai.global.tool;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import dukku.ai.global.policy.AiSimilarityPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationTool {

    private final VectorStore vectorStore;

    @Tool(description = "사용자가 요청한 검색 키워드(또는 선호도/구매이력)를 기반으로 관련 상품을 VectorStore에서 검색합니다")
    public List<String> recommendProducts(
            ToolContext context,
            @ToolParam(description = "사용자가 찾고자 하는 상품의 검색 키워드 (예: 캠핑 의자, 텐트 등)") String searchKeyword) {
        String userId = context.getContext().get("userId").toString();
        log.info("[Tool 호출] 상품 추천: userId={}, keyword={}", userId, searchKeyword);
        try {
            UUID userUuid = UUID.fromString(userId);
            String query = searchKeyword;
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(AiSimilarityPolicy.RECOMMENDATION_TOP_K)
                    .similarityThreshold(AiSimilarityPolicy.RECOMMENDATION_SIMILARITY_THRESHOLD)
                    .build();

            List<Document> docs = vectorStore.similaritySearch(request);

            List<String> results = docs.stream()
                    .map(Document::getText)
                    .toList();

            log.info("[RecommendationTool] {}건 상품 검색 완료", results.size());
            return results;
        } catch (IllegalArgumentException e) {
            log.warn("[RecommendationTool] 상품 추천 실패: userId={}, error=Invalid UUID string: {}", userId, userId);
            return List.of("ERROR: 올바른 사용자 UUID 형식이 아닙니다. 임의의 값을 지어내지 말고, 시스템 프롬프트에 제공된 '{user_uuid}' 값을 그대로 사용해 다시 호출하세요.");
        } catch (Exception e) {
            log.warn("[RecommendationTool] 상품 추천 실패: userId={}, error={}", userId, e.getMessage());
            return List.of();
        }
    }
}
