package dukku.ai.app.usecase;

import dukku.ai.out.HybridSearchRepository;
import dukku.common.shared.product.dto.product.ProductPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductVectorSyncUseCase {

    private final VectorStore vectorStore;
    private final HybridSearchRepository hybridSearchRepository;

    public void upsertProduct(ProductPayload payload) {
        String documentId = payload.productUuid().toString();

        // 기존 문서 삭제 후 재추가 (upsert)
        vectorStore.delete(List.of(documentId));

        String content = buildContent(payload);
        Map<String, Object> metadata = buildMetadata(payload);

        Document document = new Document(documentId, content, metadata);
        vectorStore.add(List.of(document));

        // product_search 테이블에도 동기화
        float[] embedding = hybridSearchRepository.embed(content);
        String metadataJson = toJsonString(metadata);
        hybridSearchRepository.upsert(payload.productUuid(), content, metadataJson, embedding);

        log.info("[ProductVectorSync] 상품 동기화 완료: productUuid={}", documentId);
    }

    public void deleteProduct(UUID productUuid) {
        vectorStore.delete(List.of(productUuid.toString()));
        hybridSearchRepository.delete(productUuid);
        log.info("[ProductVectorSync] 상품 삭제 완료: productUuid={}", productUuid);
    }

    private String buildContent(ProductPayload payload) {
        StringBuilder sb = new StringBuilder();
        sb.append(payload.title());
        if (payload.description() != null && !payload.description().isBlank()) {
            sb.append(" - ").append(payload.description());
        }
        sb.append(", 카테고리: ").append(payload.categoryName());
        if (payload.tags() != null && !payload.tags().isEmpty()) {
            sb.append(", 태그: ").append(String.join(", ", payload.tags()));
        }
        return sb.toString();
    }

    private Map<String, Object> buildMetadata(ProductPayload payload) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("productUuid", payload.productUuid().toString());
        metadata.put("price", payload.price());
        metadata.put("shippingFee", payload.shippingFee());
        metadata.put("categoryId", payload.categoryId());
        metadata.put("categoryName", payload.categoryName());
        metadata.put("saleStatus", payload.saleStatus().name());
        if (payload.tags() != null) {
            metadata.put("tags", String.join(",", payload.tags()));
        }
        return metadata;
    }

    private String toJsonString(Map<String, Object> metadata) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(((String) value).replace("\"", "\\\"")).append("\"");
            } else {
                sb.append(value);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
