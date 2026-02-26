package dukku.product.boundedContext.product.app.usecase.shop;

import dukku.common.shared.product.type.SaleStatus;
import dukku.product.boundedContext.product.app.support.ProductMapper;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.entity.ProductSeller;
import dukku.product.boundedContext.product.out.ProductRepository;
import dukku.product.boundedContext.product.out.ProductSellerRepository;
import dukku.common.shared.product.dto.shop.ShopProductListResponse;
import dukku.common.shared.product.dto.product.ProductListItemResponse;
import dukku.common.shared.product.exception.ProductSellerNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FindShopProductsUseCase {

    private final ProductSellerRepository productSellerRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ShopProductListResponse execute(UUID shopUuid, SaleStatus saleStatus, int page, int size) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        ProductSeller seller = productSellerRepository.findByUuid(shopUuid)
                .orElseThrow(ProductSellerNotFoundException::new);

        UUID sellerUuid = seller.getSellerUuid();
        UUID sellerUserUuid = seller.getUserUuid();

        Page<Product> result = (saleStatus == null)
                ? productRepository.findBySellerUuidAndDeletedAtIsNull(sellerUuid, pageable)
                : productRepository.findBySellerUuidAndSaleStatusAndDeletedAtIsNull(
                sellerUuid,
                saleStatus,
                pageable
        );

        // 레거시 호환: 과거 데이터 중 product.sellerUuid=userUuid로 저장된 경우 fallback
        if (result.isEmpty() && !sellerUuid.equals(sellerUserUuid)) {
            result = (saleStatus == null)
                    ? productRepository.findBySellerUuidAndDeletedAtIsNull(sellerUserUuid, pageable)
                    : productRepository.findBySellerUuidAndSaleStatusAndDeletedAtIsNull(
                    sellerUserUuid,
                    saleStatus,
                    pageable
            );
        }

        List<ProductListItemResponse> items = result.getContent().stream()
                .map(ProductMapper::toListItem)
                .toList();

        return ShopProductListResponse.from(result, items);
    }
}
