package dukku.product.boundedContext.product.app.cqrs;

import dukku.common.shared.product.exception.ProductNotFoundException;
import dukku.product.boundedContext.product.entity.query.ProductDocument;
import dukku.product.boundedContext.product.out.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteProductSyncUseCase {
    private final ProductSearchRepository productSearchRepository;

    @Transactional(readOnly = true)
    public void deletedProductToElasticsearch(Long productId) {
        // 3. Document 변환
        // 이때 product의 visibilityStatus가 HIDDEN이거나 deletedAt이 있으면 그대로 ES 문서에 반영됨
        ProductDocument document = productSearchRepository.findById(String.valueOf(productId))
                .orElseThrow(ProductNotFoundException::new);

        // 4. Elasticsearch 저장 (Upsert: 기존 ID가 있으면 덮어쓰기)
        productSearchRepository.save(document);

        log.info("제품을 ES에 성공적으로 동기화했습니다. ID: {}, 상태: {}", productId, document.getVisibilityStatus());
    }
}
