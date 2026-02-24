package dukku.product.boundedContext.product.in.listener;

import dukku.common.shared.order.event.OrderProductSaleConfirmedEvent;
import dukku.common.shared.order.event.OrderProductSaleReleasedEvent;
import dukku.common.shared.product.dto.cqrs.ProductStatDto;
import dukku.common.shared.product.event.ProductStatsBulkUpdatedEvent;
import dukku.product.boundedContext.product.app.cqrs.ProductSyncFacade;
import dukku.product.boundedContext.product.app.cqrs.SaveToElasticSearchUseCase;
import dukku.product.boundedContext.product.app.cqrs.SyncSearchProductStatsUseCase;
import dukku.product.boundedContext.product.app.usecase.product.ConfirmProductSaleUseCase;
import dukku.product.boundedContext.product.app.usecase.product.ReleaseProductReservationUseCase;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.out.ProductRepository;
import dukku.product.global.event.ProductCreatedEvent;
import dukku.product.global.event.ProductDeletedEvent;
import dukku.product.global.event.ProductUpdatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductEventListenerTest {

    @Mock
    private ProductSyncFacade productSyncFacade;

    @Mock
    private SaveToElasticSearchUseCase saveToElasticSearchUseCase;

    @Mock
    private SyncSearchProductStatsUseCase syncSearchProductStatsUseCase;

    @Mock
    private ConfirmProductSaleUseCase confirmProductSaleUseCase;

    @Mock
    private ReleaseProductReservationUseCase releaseProductReservationUseCase;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductEventListener listener;

    @Test
    @DisplayName("product.created 이벤트 수신 시 상품을 조회해 ES 동기화를 호출한다")
    void syncCreateDelegatesToSaveUseCase() {
        Product product = mock(Product.class);
        when(productRepository.findByIdWithImagesAndCategory(1)).thenReturn(Optional.of(product));
        when(productRepository.preloadProductTagsByProductId(1)).thenReturn(List.of(1L));
        when(product.getTagNames()).thenReturn(List.of("tag"));

        listener.syncCreate(new ProductCreatedEvent(1));

        verify(productRepository).preloadProductTagsByProductId(1);
        verify(saveToElasticSearchUseCase).execute(product, true);
    }

    @Test
    @DisplayName("product.updated 이벤트 수신 시 category 변경 여부를 함께 전달한다")
    void syncUpdateDelegatesWithCategoryChangedFlag() {
        Product product = mock(Product.class);
        when(productRepository.findByIdWithImagesAndCategory(2)).thenReturn(Optional.of(product));
        when(productRepository.preloadProductTagsByProductId(2)).thenReturn(List.of(1L));
        when(product.getTagNames()).thenReturn(List.of("tag"));

        listener.syncUpdate(new ProductUpdatedEvent(2, false));

        verify(productRepository).preloadProductTagsByProductId(2);
        verify(saveToElasticSearchUseCase).execute(product, false);
    }

    @Test
    @DisplayName("product.deleted 이벤트 수신 시 삭제 동기화를 호출한다")
    void syncDeleteDelegatesToSyncFacade() {
        listener.syncDelete(new ProductDeletedEvent(3));

        verify(productSyncFacade).syncProductToElasticsearch(3L);
    }

    @Test
    @DisplayName("product.stats-updated 이벤트 수신 시 통계 동기화를 호출한다")
    void syncStatsDelegatesToStatsUseCase() {
        List<ProductStatDto> stats = List.of(new ProductStatDto(1, 10L, 2L, 1L));

        listener.syncStats(new ProductStatsBulkUpdatedEvent(stats));

        verify(syncSearchProductStatsUseCase).execute(stats);
    }

    @Test
    @DisplayName("order.product-sale-confirmed 이벤트 수신 시 판매 확정 유스케이스를 호출한다")
    void handleOrderConfirmedDelegatesToUseCase() {
        UUID orderUuid = UUID.randomUUID();
        List<UUID> productUuids = List.of(UUID.randomUUID());

        listener.handleOrderConfirmed(new OrderProductSaleConfirmedEvent(orderUuid, productUuids));

        verify(confirmProductSaleUseCase).execute(orderUuid, productUuids);
    }

    @Test
    @DisplayName("order.product-sale-released 이벤트 수신 시 예약 해제 유스케이스를 호출한다")
    void handleOrderReleasedDelegatesToUseCase() {
        UUID orderUuid = UUID.randomUUID();
        List<UUID> productUuids = List.of(UUID.randomUUID());

        listener.handleOrderReleased(new OrderProductSaleReleasedEvent(orderUuid, productUuids));

        verify(releaseProductReservationUseCase).execute(orderUuid, productUuids);
    }
}
