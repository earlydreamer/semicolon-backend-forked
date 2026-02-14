package dukku.ai.app.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentRetrievalService {

    private static final int TOP_K = 5;
    private static final double DOCUMENT_SIMILARITY_THRESHOLD = 0.7;

    private static final Set<String> RETRIEVAL_KEYWORDS = Set.of(
            "상품", "추천", "환불", "정책", "배송", "교환", "가격", "할인",
            "쿠폰", "결제", "주문", "반품", "사이즈", "재고", "품절"
    );

    private final VectorStore vectorStore;

    public String retrieve(String userMessage) {
        if (!requiresRetrieval(userMessage)) {
            log.debug("[DocumentRetrieval] 검색 불필요 - 스킵: {}", userMessage);
            return "";
        }

        SearchRequest request = SearchRequest.builder()
                .query(userMessage)
                .topK(TOP_K)
                .similarityThreshold(DOCUMENT_SIMILARITY_THRESHOLD)
                .build();

        List<Document> docs = vectorStore.similaritySearch(request);

        if (docs.isEmpty()) {
            log.debug("[DocumentRetrieval] 검색 결과 없음");
            return "";
        }

        log.debug("[DocumentRetrieval] {}건 검색 완료", docs.size());
        return formatDocuments(docs);
    }

    private boolean requiresRetrieval(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        return RETRIEVAL_KEYWORDS.stream()
                .anyMatch(userMessage::contains);
    }

    private String formatDocuments(List<Document> docs) {
        return docs.stream()
                .map(doc -> "- " + doc.getText())
                .collect(Collectors.joining("\n", "## 관련 문서\n", ""));
    }
}
