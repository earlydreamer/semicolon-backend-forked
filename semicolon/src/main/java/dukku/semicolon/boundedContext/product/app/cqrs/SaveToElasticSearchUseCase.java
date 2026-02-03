package dukku.semicolon.boundedContext.product.app.cqrs;

import dukku.common.shared.product.type.SaleStatus;
import dukku.semicolon.boundedContext.product.entity.Product;
import dukku.semicolon.boundedContext.product.entity.ProductImage;
import dukku.semicolon.boundedContext.product.entity.query.ProductDocument;
import dukku.semicolon.boundedContext.product.out.CategoryRepository;
import dukku.semicolon.boundedContext.product.out.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations; // [추가]
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
        } else {
            updatePartialDocument(product);
        }
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
                .build();

        // 전체 저장은 Repository가 편합니다.
        productSearchRepository.save(document);
        log.info("Full Sync Completed: {}", product.getId());
    }

    // [Case 2] 부분 업데이트 (ElasticsearchOperations 사용)
    private void updatePartialDocument(Product product) {
        String docId = String.valueOf(product.getId());
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

        // deletedAt 등 변경 가능성 있는 다른 필드도 필요하면 추가
        UpdateQuery updateQuery = UpdateQuery.builder(docId)
                .withDocument(document)
                .withDocAsUpsert(true)
                .build();

        // getIndexCoordinatesFor()를 사용해 인덱스 정보를 추출해서 전달
        elasticsearchOperations.update(
                updateQuery,
                elasticsearchOperations.getIndexCoordinatesFor(ProductDocument.class)
        );

        log.info("Partial Sync Completed: {}", product.getId());
    }

    private String getThumbnailUrl(Product product) {
        return product.getImages().stream()
                .filter(ProductImage::isThumbnail)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(null);
    }
}