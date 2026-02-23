package dukku.ai.app.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import dukku.ai.entity.AiUserMemory;
import dukku.ai.global.policy.AiSimilarityPolicy;
import dukku.ai.out.AiMemoryRepository;
import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 장바구니 이벤트 기반 비동기 추천 서비스
 * VectorStore에서 유사 상품을 검색하고 AiMemory에 추천 결과를 저장합니다.
 * LLM 호출 없이 VectorStore 직접 검색만 수행하여 비용/지연을 최소화합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartRecommendationUseCase {

    private final VectorStore vectorStore;
    private final AiMemoryRepository aiMemoryRepository;
    private final EmbeddingModel embeddingModel;

    @Async
    public void generateRecommendation(UUID userUuid, String productTitle) {
        try {
            log.info("[CartRecommendation] 추천 생성 시작: userId={}, product={}", userUuid, productTitle);

            // 1. VectorStore에서 유사 상품 검색
            SearchRequest request = SearchRequest.builder()
                    .query(productTitle)
                    .topK(AiSimilarityPolicy.RECOMMENDATION_TOP_K)
                    .similarityThreshold(AiSimilarityPolicy.RECOMMENDATION_SIMILARITY_THRESHOLD)
                    .build();

            List<Document> docs = vectorStore.similaritySearch(request);

            if (docs.isEmpty()) {
                log.info("[CartRecommendation] 유사 상품 없음: userId={}, product={}", userUuid, productTitle);
                return;
            }

            List<String> recommendedProducts = docs.stream()
                    .map(Document::getText)
                    .toList();

            // 2. 추천 결과를 AiMemory에 저장
            String content = "장바구니 추가 기반 추천 (기준: %s): %s".formatted(
                    productTitle, String.join(", ", recommendedProducts));
            float[] embedding = embeddingModel.embed(content);

            AiUserMemory memory = AiUserMemory.builder()
                    .userUuid(userUuid)
                    .memoryType(MemoryType.RECOMMENDATION)
                    .subType(MemorySubType.SHOPPING)
                    .content(content)
                    .embedding(embedding)
                    .importanceScore(0.7)
                    .build();

            aiMemoryRepository.save(memory);

            log.info("[CartRecommendation] 추천 {}건 저장 완료: userId={}", recommendedProducts.size(), userUuid);

        } catch (Exception e) {
            log.error("[CartRecommendation] 추천 생성 실패: userId={}, error={}", userUuid, e.getMessage(), e);
        }
    }
}
