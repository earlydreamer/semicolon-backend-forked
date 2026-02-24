package dukku.product.boundedContext.product.app.cqrs;

import dukku.common.shared.product.type.SaleStatus;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.entity.ProductImage;
import dukku.product.boundedContext.product.entity.query.ProductDocument;
import dukku.product.boundedContext.product.out.CategoryRepository;
import dukku.product.boundedContext.product.out.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaveToElasticSearchUseCase {
    private final ProductSearchRepository productSearchRepository;
    private final CategoryRepository categoryRepository;
    // 부분 업데이트(update)는 Repository가 아니라 Operations가 담당
    private final ElasticsearchOperations elasticsearchOperations;

    // 생성 시 편의 메서드 (Create)
    @Transactional(readOnly = true)
    public void execute(Product product) {
        execute(product, true);
    }

    // 수정 시 메인 메서드 (Update)
    @Transactional(readOnly = true)
    public void execute(Product product, boolean isCategoryUpdated) {
        if (isCategoryUpdated) {
            saveFullDocument(product);
            return;
        }
        updatePartialDocument(product);
    }

    // [Case 1] 전체 저장 (Repository 사용)
    private void saveFullDocument(Product product) {
        List<Integer> categoryPathIds = categoryRepository.findCategoryPathIds(product.getCategory().getId());
        String thumbnail = getThumbnailUrl(product);
        int sortPriority = (product.getSaleStatus() == SaleStatus.SOLD_OUT) ? 1 : 0;

        ProductDocument document = ProductDocument.builder()
                .id(String.valueOf(product.getId()))
                .productUuid(product.getUuid().toString())
                .sellerUuid(product.getSellerUuid().toString())
                .categoryIds(categoryPathIds)
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .shippingFee(product.getShippingFee())
                .conditionStatus(product.getConditionStatus())
                .saleStatus(product.getSaleStatus())
                .visibilityStatus(product.getVisibilityStatus())
                .saleSortPriority(sortPriority)
                .likeCount(product.getLikeCount())
                .viewCount(product.getViewCount())
                .commentCount(product.getCommentCount())
                .createdAt(product.getCreatedAt())
                .deletedAt(product.getDeletedAt())
                .thumbnailImageUrl(thumbnail)
                .tags(product.getTagNames())
                .build();

        // 전체 저장은 Repository가 편합니다.
        productSearchRepository.save(document);
        log.info("[ES 동기화] 전체 저장 완료. productId={}", product.getId());
    }

    // [Case 2] 부분 업데이트 (ElasticsearchOperations 사용)
    private void updatePartialDocument(Product product) {
        String docId = String.valueOf(product.getId());

        if (!productSearchRepository.existsById(docId)) {
            log.warn("[ES 동기화] 부분 업데이트 대상 문서 없음 -> 전체 저장으로 전환. productId={}", product.getId());
            saveFullDocument(product);
            return;
        }

        String thumbnail = getThumbnailUrl(product);
        int sortPriority = (product.getSaleStatus() == SaleStatus.SOLD_OUT) ? 1 : 0;

        Document document = Document.create();
        document.put("title", product.getTitle());
        document.put("description", product.getDescription());
        document.put("price", product.getPrice());
        document.put("shippingFee", product.getShippingFee());
        document.put("conditionStatus", product.getConditionStatus());
        document.put("saleStatus", product.getSaleStatus());
        document.put("visibilityStatus", product.getVisibilityStatus());
        document.put("saleSortPriority", sortPriority);
        document.put("thumbnailImageUrl", thumbnail);
        document.put("tags", product.getTagNames());

        UpdateQuery updateQuery = UpdateQuery.builder(docId)
                .withDocument(document)
                .withDocAsUpsert(true)
                .build();

        // getIndexCoordinatesFor()를 사용해 인덱스 정보를 추출해서 전달
        elasticsearchOperations.update(
                updateQuery,
                elasticsearchOperations.getIndexCoordinatesFor(ProductDocument.class)
        );

        log.info("[ES 동기화] 부분 업데이트 완료. productId={}", product.getId());
    }

    private String getThumbnailUrl(Product product) {
        return product.getImages().stream()
                .filter(ProductImage::isThumbnail)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(null);
    }
}
