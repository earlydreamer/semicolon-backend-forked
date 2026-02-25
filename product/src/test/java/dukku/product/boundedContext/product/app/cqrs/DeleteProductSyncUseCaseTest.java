package dukku.product.boundedContext.product.app.cqrs;

import dukku.common.shared.product.type.VisibilityStatus;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.entity.query.ProductDocument;
import dukku.product.boundedContext.product.out.ProductRepository;
import dukku.product.boundedContext.product.out.ProductSearchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteProductSyncUseCaseTest {

    @Mock
    private ProductSearchRepository productSearchRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private DeleteProductSyncUseCase useCase;

    @Test
    @DisplayName("Soft-deletes document when ES document exists")
    void softDeleteWhenDocumentExists() {
        LocalDateTime deletedAt = LocalDateTime.of(2026, 2, 25, 3, 0, 8);

        ProductDocument existing = ProductDocument.builder()
                .id("42")
                .productUuid("019c8f6d-d692-7009-9300-f1241e7ad809")
                .visibilityStatus(VisibilityStatus.VISIBLE)
                .build();

        Product product = org.mockito.Mockito.mock(Product.class);
        when(product.getDeletedAt()).thenReturn(deletedAt);

        when(productSearchRepository.findById("42")).thenReturn(Optional.of(existing));
        when(productRepository.findById(42)).thenReturn(Optional.of(product));

        useCase.deletedProductToElasticsearch(42L);

        ArgumentCaptor<ProductDocument> captor = ArgumentCaptor.forClass(ProductDocument.class);
        verify(productSearchRepository).save(captor.capture());

        ProductDocument saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo("42");
        assertThat(saved.getVisibilityStatus()).isEqualTo(VisibilityStatus.HIDDEN);
        assertThat(saved.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    @DisplayName("Skips sync when ES document is missing")
    void skipWhenDocumentMissing() {
        when(productSearchRepository.findById("42")).thenReturn(Optional.empty());

        useCase.deletedProductToElasticsearch(42L);

        verify(productRepository, never()).findById(42);
        verify(productSearchRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
