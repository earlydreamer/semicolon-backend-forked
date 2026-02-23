package dukku.ai.app.usecase;

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

    public void upsertProduct(ProductPayload payload) {
        String documentId = payload.productUuid().toString();

        // 기존 문서 삭제 후 재추가 (upsert)
        vectorStore.delete(List.of(documentId));

        String content = buildContent(payload);
        Map<String, Object> metadata = buildMetadata(payload);

        Document document = new Document(documentId, content, metadata);
        vectorStore.add(List.of(document));

        log.info("[ProductVectorSync] 상품 동기화 완료: productUuid={}", documentId);
    }

    public void deleteProduct(UUID productUuid) {
        vectorStore.delete(List.of(productUuid.toString()));
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
}
