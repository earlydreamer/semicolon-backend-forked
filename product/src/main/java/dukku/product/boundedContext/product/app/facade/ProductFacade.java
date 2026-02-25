package dukku.product.boundedContext.product.app.facade;

import dukku.common.shared.product.dto.cqrs.ProductSearchRequest;
import dukku.common.shared.product.dto.cqrs.ProductSortType;
import dukku.common.shared.product.dto.product.CategoryCreateResponse;
import dukku.common.shared.product.dto.product.ProductDetailResponse;
import dukku.common.shared.product.dto.product.ProductListItemResponse;
import dukku.common.shared.product.dto.product.ProductListResponse;
import dukku.common.shared.product.dto.product.ProductReserveRequest;
import dukku.product.boundedContext.product.app.cqrs.SearchProductUseCase;
import dukku.product.boundedContext.product.app.usecase.product.FindCategoryListUseCase;
import dukku.product.boundedContext.product.app.usecase.product.FindFeaturedProductsUseCase;
import dukku.product.boundedContext.product.app.usecase.product.FindProductDetailUseCase;
import dukku.product.boundedContext.product.app.usecase.product.FindProductListUseCase;
import dukku.product.boundedContext.product.app.usecase.product.ReleaseProductReservationUseCase;
import dukku.product.boundedContext.product.app.usecase.product.ReserveProductUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductFacade {
    private final FindCategoryListUseCase findCategoryListUseCase;
    private final FindFeaturedProductsUseCase findFeaturedProductsUseCase;
    private final FindProductListUseCase findProductListUseCase;
    private final FindProductDetailUseCase findProductDetailUseCase;
    private final ReserveProductUseCase reserveProductUseCase;
    private final ReleaseProductReservationUseCase releaseProductReservationUseCase;
    private final SearchProductUseCase searchProductUseCase;

    public List<CategoryCreateResponse> findCategories() {
        return findCategoryListUseCase.execute();
    }

    public List<ProductListItemResponse> findFeatured(int size) {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setSortType(ProductSortType.LIKES);

        Pageable pageable = PageRequest.of(0, size);

        try {
            List<ProductListItemResponse> items = searchProductUseCase.searchProducts(request, pageable).getItems();
            if (items.isEmpty()) {
                log.warn("상품 조회 소스=DB 폴백 (featured, empty-result), size={}", size);
                return findFeaturedProductsUseCase.execute(size);
            }
            log.info("상품 조회 소스=ES (featured), size={}, resultCount={}", size, items.size());
            return items;
        } catch (Exception e) {
            log.warn("상품 조회 소스=DB 폴백 (featured), size={}", size, e);
            return findFeaturedProductsUseCase.execute(size);
        }
    }

    public ProductListResponse findProducts(ProductSearchRequest request, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(size, 50));

        try {
            ProductListResponse response = searchProductUseCase.searchProducts(request, pageable);
            if (response.getItems() == null || response.getItems().isEmpty()) {
                log.warn("상품 조회 소스=DB 폴백 (empty-result), page={}, size={}, categoryId={}",
                        pageable.getPageNumber(), pageable.getPageSize(), request.getCategoryId());
                return findProductListUseCase.execute(request.getCategoryId(), pageable);
            }
            log.info("상품 조회 소스=ES, page={}, size={}, categoryId={}, totalCount={}",
                    pageable.getPageNumber(), pageable.getPageSize(), request.getCategoryId(), response.getTotalCount());
            return response;
        } catch (Exception e) {
            log.warn("상품 조회 소스=DB 폴백, page={}, size={}, categoryId={}",
                    pageable.getPageNumber(), pageable.getPageSize(), request.getCategoryId(), e);
            return findProductListUseCase.execute(request.getCategoryId(), pageable);
        }
    }

    public ProductDetailResponse findProductDetail(UUID productUuid) {
        return findProductDetailUseCase.execute(productUuid);
    }

    public void reserveProducts(ProductReserveRequest request) {
        reserveProductUseCase.execute(request);
    }

    public void releaseProducts(ProductReserveRequest request) {
        releaseProductReservationUseCase.execute(request.orderUuid(), request.productUuids());
    }
}
