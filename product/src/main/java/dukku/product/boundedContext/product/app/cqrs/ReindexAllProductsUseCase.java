package dukku.product.boundedContext.product.app.cqrs;

import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.out.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReindexAllProductsUseCase {
    private static final int BATCH_SIZE = 200;

    private final ProductRepository productRepository;
    private final SaveToElasticSearchUseCase saveToElasticSearchUseCase;

    @Transactional(readOnly = true)
    public void execute() {
        int pageNo = 0;
        int success = 0;
        int fail = 0;

        log.info("[Reindex] 전체 재색인 시작");

        while (true) {
            Pageable pageable = PageRequest.of(pageNo, BATCH_SIZE);
            Page<Integer> idPage = productRepository.findActiveProductIds(pageable);

            if (idPage.isEmpty()) {
                break;
            }

            for (Integer productId : idPage.getContent()) {
                try {
                    Product product = productRepository.findByIdWithImagesAndCategory(productId)
                            .orElse(null);

                    if (product == null) {
                        continue;
                    }

                    productRepository.preloadProductTagsByProductId(productId);
                    product.getTagNames();
                    saveToElasticSearchUseCase.execute(product, true);
                    success++;
                } catch (Exception e) {
                    fail++;
                    log.error("[Reindex] 상품 재색인 실패: productId={}", productId, e);
                }
            }

            if (!idPage.hasNext()) {
                break;
            }

            pageNo++;
        }

        log.info("[Reindex] 전체 재색인 완료: success={}, fail={}", success, fail);
    }
}
