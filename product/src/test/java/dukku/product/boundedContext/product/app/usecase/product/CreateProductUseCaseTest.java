package dukku.product.boundedContext.product.app.usecase.product;

import dukku.common.global.event.DomainEvent;
import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.product.dto.product.ProductCreateRequest;
import dukku.common.shared.product.exception.ProductCategoryNotFoundException;
import dukku.common.shared.product.type.ConditionStatus;
import dukku.product.boundedContext.product.app.support.ProductSupport;
import dukku.product.boundedContext.product.app.support.ProductTagSupport;
import dukku.product.boundedContext.product.entity.Category;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.out.CategoryRepository;
import dukku.product.boundedContext.product.out.ProductRepository;
import dukku.product.global.event.ProductCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProductUseCaseTest {

    @Mock
    private ProductSupport productSupport;

    @Mock
    private ProductTagSupport productTagSupport;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CreateProductUseCase useCase;

    @Test
    @DisplayName("카테고리가 없으면 ProductCategoryNotFoundException을 던진다")
    void executeThrowsWhenCategoryDoesNotExist() {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .categoryId(777)
                .title("title")
                .description("desc")
                .price(1000L)
                .shippingFee(0L)
                .conditionStatus(ConditionStatus.SEALED)
                .tags(List.of())
                .build();

        when(categoryRepository.findById(777)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), request))
                .isInstanceOf(ProductCategoryNotFoundException.class);

        verify(categoryRepository).findById(777);
        verifyNoInteractions(productRepository, eventPublisher);
    }

    @Test
    @DisplayName("정상 생성 시 카테고리를 즉시 로딩하고 created/sync 이벤트를 발행한다")
    void executePublishesCreatedAndSyncEvents() {
        UUID sellerUuid = UUID.randomUUID();
        ProductCreateRequest request = ProductCreateRequest.builder()
                .categoryId(3)
                .title("title")
                .description("desc")
                .price(1000L)
                .shippingFee(0L)
                .conditionStatus(ConditionStatus.SEALED)
                .tags(List.of())
                .build();

        Category category = org.mockito.Mockito.mock(Category.class);
        when(category.getId()).thenReturn(3);
        when(category.getDepth()).thenReturn(2);
        when(category.getCategoryName()).thenReturn("디지털");

        when(categoryRepository.findById(3)).thenReturn(Optional.of(category));
        when(productTagSupport.getOrCreateTags(List.of())).thenReturn(List.of());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.prePersist();
            return saved;
        });

        useCase.execute(sellerUuid, request);

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publish(captor.capture());
        List<DomainEvent> published = captor.getAllValues();

        assertThat(published.get(0)).isInstanceOf(ProductCreatedEvent.class);
        assertThat(published.get(1).getTopic()).isEqualTo("product-events");
        verify(categoryRepository).findById(3);
    }
}
