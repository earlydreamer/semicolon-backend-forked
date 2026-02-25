package dukku.ai.app.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import dukku.ai.app.dto.HybridSearchResult;
import dukku.ai.global.policy.AiPromptPolicy;
import dukku.ai.global.policy.AiSimilarityPolicy;
import dukku.ai.out.HybridSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRetrievalService {

    private final HybridSearchRepository hybridSearchRepository;

    public String retrieve(String userMessage) {
        if (!requiresRetrieval(userMessage)) {
            log.info("[상품 검색] 키워드 미감지 → 스킵: {}", userMessage);
            return "";
        }

        String matchedKeyword = AiPromptPolicy.DOCUMENT_RETRIEVAL_KEYWORDS.stream()
                .filter(userMessage::contains)
                .findFirst()
                .orElse("?");
        log.info("[상품 검색] 키워드 감지: '{}' → 하이브리드 검색 실행", matchedKeyword);

        log.info("[상품 검색] 하이브리드 검색 실행: query={}, topK={}", userMessage, AiSimilarityPolicy.DOCUMENT_TOP_K);

        List<HybridSearchResult> results = hybridSearchRepository.search(
                userMessage,
                AiSimilarityPolicy.DOCUMENT_TOP_K,
                AiSimilarityPolicy.DOCUMENT_SIMILARITY_THRESHOLD);

        if (results.isEmpty()) {
            log.info("[상품 검색] 검색 결과 없음");
            return "";
        }

        log.info("[상품 검색] {}건 검색 완료", results.size());
        results.forEach(r -> log.info("[상품 검색]   - [rrf={}, vector={}, keyword={}] {}",
                String.format("%.4f", r.rrfScore()),
                String.format("%.4f", r.vectorScore()),
                String.format("%.4f", r.keywordScore()),
                r.content()));
        return formatResults(results);
    }

    private boolean requiresRetrieval(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        return AiPromptPolicy.DOCUMENT_RETRIEVAL_KEYWORDS.stream()
                .anyMatch(userMessage::contains);
    }

    private String formatResults(List<HybridSearchResult> results) {
        return results.stream()
                .map(r -> "- " + r.contentWithUrl())
                .collect(Collectors.joining("\n", "## 관련 문서\n", ""));
    }
}
