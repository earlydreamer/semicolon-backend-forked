package dukku.ai.app.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import dukku.ai.global.policy.AiPromptPolicy;
import dukku.ai.global.policy.AiSimilarityPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRetrievalService {

    private final VectorStore vectorStore;

    public String retrieve(String userMessage) {
        if (!requiresRetrieval(userMessage)) {
            log.info("[상품 검색] 키워드 미감지 → 스킵: {}", userMessage);
            return "";
        }

        String matchedKeyword = AiPromptPolicy.DOCUMENT_RETRIEVAL_KEYWORDS.stream()
                .filter(userMessage::contains)
                .findFirst()
                .orElse("?");
        log.info("[상품 검색] 키워드 감지: '{}' → 검색 실행", matchedKeyword);

        SearchRequest request = SearchRequest.builder()
                .query(userMessage)
                .topK(AiSimilarityPolicy.DOCUMENT_TOP_K)
                .similarityThreshold(AiSimilarityPolicy.DOCUMENT_SIMILARITY_THRESHOLD)
                .build();

        log.info("[상품 검색] query={}, topK={}, threshold={}", userMessage, AiSimilarityPolicy.DOCUMENT_TOP_K, AiSimilarityPolicy.DOCUMENT_SIMILARITY_THRESHOLD);

        List<Document> docs = vectorStore.similaritySearch(request);

        if (docs.isEmpty()) {
            log.info("[상품 검색] 검색 결과 없음");
            return "";
        }

        log.info("[상품 검색] {}건 검색 완료", docs.size());
        docs.forEach(doc -> log.info("[상품 검색]   - [score={}] {}", String.format("%.4f", doc.getScore()), doc.getText()));
        return formatDocuments(docs);
    }

    private boolean requiresRetrieval(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        return AiPromptPolicy.DOCUMENT_RETRIEVAL_KEYWORDS.stream()
                .anyMatch(userMessage::contains);
    }

    private String formatDocuments(List<Document> docs) {
        return docs.stream()
                .map(doc -> "- " + doc.getText())
                .collect(Collectors.joining("\n", "## 관련 문서\n", ""));
    }
}
