package dukku.ai.app.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import dukku.common.shared.ai.dto.HybridSearchResult;
import dukku.ai.entity.AiUserMemory;
import dukku.ai.global.policy.AiSimilarityPolicy;
import dukku.ai.out.AiUserMemoryRepository;
import dukku.ai.out.HybridSearchRepository;
import dukku.ai.app.service.GeminiEmbeddingService;
import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 장바구니 이벤트 기반 비동기 추천 서비스
 * HybridSearchRepository에서 유사 상품을 검색하고 AiMemory에 추천 결과를 저장합니다.
 * LLM 호출 없이 직접 검색만 수행하여 비용/지연을 최소화합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartRecommendationUseCase {

    private final HybridSearchRepository hybridSearchRepository;
    private final AiUserMemoryRepository aiUserMemoryRepository;
    private final GeminiEmbeddingService embeddingService;

    @Async
    public void generateRecommendation(UUID userUuid, String productTitle) {
        try {
            log.info("[CartRecommendation] 추천 생성 시작: userId={}, productTitleLength={}",
                    userUuid, productTitle != null ? productTitle.length() : 0);

            List<HybridSearchResult> results = hybridSearchRepository.search(
                    productTitle,
                    AiSimilarityPolicy.RECOMMENDATION_TOP_K,
                    AiSimilarityPolicy.RECOMMENDATION_SIMILARITY_THRESHOLD);

            if (results.isEmpty()) {
                log.info("[CartRecommendation] 유사 상품 없음: userId={}, productTitleLength={}",
                        userUuid, productTitle != null ? productTitle.length() : 0);
                return;
            }

            List<String> recommendedProducts = results.stream()
                    .map(HybridSearchResult::content)
                    .toList();

            // 추천 결과를 AiMemory에 저장
            String content = "장바구니 추가 기반 추천 (기준: %s): %s".formatted(
                    productTitle, String.join(", ", recommendedProducts));
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

            log.info("[CartRecommendation] 추천 {}건 저장 완료: userId={}", recommendedProducts.size(), userUuid);

        } catch (Exception e) {
            log.error("[CartRecommendation] 추천 생성 실패: userId={}, errorType={}",
                    userUuid, e.getClass().getSimpleName());
        }
    }
}
