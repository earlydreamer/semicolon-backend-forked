package dukku.ai.global.tool;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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

    @Tool(description = "사용자의 구매 이력과 선호도를 기반으로 유사한 상품을 VectorStore에서 검색합니다")
    public List<String> recommendProducts(
            @ToolParam(description = "사용자 UUID") UUID userId,
            @ToolParam(description = "구매 이력 목록") List<String> purchaseHistory) {
        log.info("[Tool 호출] 상품 추천: userId={}, history={}", userId, purchaseHistory);
        try {
            String query = String.join(", ", purchaseHistory);
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
        } catch (Exception e) {
            log.warn("[RecommendationTool] 상품 추천 실패: userId={}, error={}", userId, e.getMessage());
            return List.of();
        }
    }
}
