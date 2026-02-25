package dukku.product.boundedContext.product.app.cqrs;

import dukku.common.shared.product.type.VisibilityStatus;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.entity.query.ProductDocument;
import dukku.product.boundedContext.product.out.ProductRepository;
import dukku.product.boundedContext.product.out.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteProductSyncUseCase {
    private final ProductSearchRepository productSearchRepository;
    private final ProductRepository productRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Transactional(readOnly = true)
    public void deletedProductToElasticsearch(Long productId) {
        String documentId = String.valueOf(productId);

        Optional<ProductDocument> existingDocument = productSearchRepository.findById(documentId);
        if (existingDocument.isEmpty()) {
            log.info("ES 문서가 없어 소프트 삭제 동기화를 건너뜁니다. productId={}", productId);
            return;
        }

        Optional<Product> product = productRepository.findById(productId.intValue());
        LocalDateTime deletedAt = product.map(Product::getDeletedAt).orElse(LocalDateTime.now());

        ProductDocument updatedDocument = toSoftDeleted(existingDocument.get(), deletedAt);
        productSearchRepository.save(updatedDocument);
        elasticsearchOperations.indexOps(ProductDocument.class).refresh();

        log.info("Elasticsearch 상품 문서를 소프트 삭제로 동기화했습니다. productId={}", productId);
    }

    private ProductDocument toSoftDeleted(ProductDocument source, LocalDateTime deletedAt) {
        return ProductDocument.builder()
                .id(source.getId())
                .productUuid(source.getProductUuid())
                .saleSortPriority(source.getSaleSortPriority())
                .title(source.getTitle())
                .description(source.getDescription())
                .sellerUuid(source.getSellerUuid())
                .categoryIds(source.getCategoryIds())
                .saleStatus(source.getSaleStatus())
                .visibilityStatus(VisibilityStatus.HIDDEN)
                .price(source.getPrice())
                .shippingFee(source.getShippingFee())
                .viewCount(source.getViewCount())
                .likeCount(source.getLikeCount())
                .commentCount(source.getCommentCount())
                .createdAt(source.getCreatedAt())
                .thumbnailImageUrl(source.getThumbnailImageUrl())
                .deletedAt(deletedAt)
                .conditionStatus(source.getConditionStatus())
                .tags(source.getTags())
                .build();
    }
}
