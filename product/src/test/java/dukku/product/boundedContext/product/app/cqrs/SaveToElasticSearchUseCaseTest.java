package dukku.product.boundedContext.product.app.cqrs;

import dukku.common.shared.product.type.SaleStatus;
import dukku.common.shared.product.type.VisibilityStatus;
import dukku.product.boundedContext.product.entity.Category;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.entity.query.ProductDocument;
import dukku.product.boundedContext.product.out.CategoryRepository;
import dukku.product.boundedContext.product.out.ProductSearchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveToElasticSearchUseCaseTest {

    @Mock
    private ProductSearchRepository productSearchRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private IndexOperations indexOperations;

    @InjectMocks
    private SaveToElasticSearchUseCase useCase;

    @Test
    @DisplayName("전체 저장 후 ES index refresh를 호출한다")
    void refreshAfterFullSave() {
        Product product = mock(Product.class);
        Category category = mock(Category.class);
        when(category.getId()).thenReturn(10);
        when(product.getId()).thenReturn(42);
        when(product.getUuid()).thenReturn(UUID.randomUUID());
        when(product.getSellerUuid()).thenReturn(UUID.randomUUID());
        when(product.getCategory()).thenReturn(category);
        when(product.getTitle()).thenReturn("title");
        when(product.getDescription()).thenReturn("desc");
        when(product.getPrice()).thenReturn(1000L);
        when(product.getShippingFee()).thenReturn(0L);
        when(product.getConditionStatus()).thenReturn(null);
        when(product.getSaleStatus()).thenReturn(SaleStatus.ON_SALE);
        when(product.getVisibilityStatus()).thenReturn(VisibilityStatus.VISIBLE);
        when(product.getLikeCount()).thenReturn(0);
        when(product.getViewCount()).thenReturn(0);
        when(product.getCommentCount()).thenReturn(0);
        when(product.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(product.getDeletedAt()).thenReturn(null);
        when(product.getImages()).thenReturn(List.of());
        when(product.getTagNames()).thenReturn(List.of());

        when(categoryRepository.findCategoryPathIds(10)).thenReturn(List.of(10));
        when(elasticsearchOperations.indexOps(ProductDocument.class)).thenReturn(indexOperations);

        useCase.execute(product, true);

        verify(productSearchRepository).save(any(ProductDocument.class));
        verify(indexOperations).refresh();
    }

    @Test
    @DisplayName("부분 업데이트 후 ES index refresh를 호출한다")
    void refreshAfterPartialUpdate() {
        Product product = mock(Product.class);
        Category category = mock(Category.class);
        when(category.getId()).thenReturn(10);
        when(product.getId()).thenReturn(42);
        when(product.getUuid()).thenReturn(UUID.randomUUID());
        when(product.getSellerUuid()).thenReturn(UUID.randomUUID());
        when(product.getCategory()).thenReturn(category);
        when(product.getTitle()).thenReturn("title");
        when(product.getDescription()).thenReturn("desc");
        when(product.getPrice()).thenReturn(1000L);
        when(product.getShippingFee()).thenReturn(0L);
        when(product.getConditionStatus()).thenReturn(null);
        when(product.getSaleStatus()).thenReturn(SaleStatus.ON_SALE);
        when(product.getVisibilityStatus()).thenReturn(VisibilityStatus.VISIBLE);
        when(product.getImages()).thenReturn(List.of());
        when(product.getTagNames()).thenReturn(List.of());

        when(categoryRepository.findCategoryPathIds(10)).thenReturn(List.of(10));
        when(productSearchRepository.existsById("42")).thenReturn(true);
        when(elasticsearchOperations.getIndexCoordinatesFor(ProductDocument.class))
                .thenReturn(IndexCoordinates.of("products_v1"));
        when(elasticsearchOperations.indexOps(ProductDocument.class)).thenReturn(indexOperations);

        useCase.execute(product, false);

        verify(elasticsearchOperations).update(any(), any(IndexCoordinates.class));
        verify(indexOperations).refresh();
    }
}
