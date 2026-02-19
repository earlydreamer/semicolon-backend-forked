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
public class DocumentRetrievalService {

    private final VectorStore vectorStore;

    public String retrieve(String userMessage) {
        if (!requiresRetrieval(userMessage)) {
            log.debug("[DocumentRetrieval] 검색 불필요 - 스킵: {}", userMessage);
            return "";
        }

        SearchRequest request = SearchRequest.builder()
                .query(userMessage)
                .topK(AiSimilarityPolicy.DOCUMENT_TOP_K)
                .similarityThreshold(AiSimilarityPolicy.DOCUMENT_SIMILARITY_THRESHOLD)
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
        return AiPromptPolicy.DOCUMENT_RETRIEVAL_KEYWORDS.stream()
                .anyMatch(userMessage::contains);
    }

    private String formatDocuments(List<Document> docs) {
        return docs.stream()
                .map(doc -> "- " + doc.getText())
                .collect(Collectors.joining("\n", "## 관련 문서\n", ""));
    }
}
