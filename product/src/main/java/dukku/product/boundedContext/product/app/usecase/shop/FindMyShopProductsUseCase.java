package dukku.product.boundedContext.product.app.usecase.shop;

import dukku.common.shared.product.type.SaleStatus;
import dukku.product.boundedContext.product.app.support.ProductMapper;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.entity.ProductSeller;
import dukku.product.boundedContext.product.out.ProductRepository;
import dukku.product.boundedContext.product.out.ProductSellerRepository;
import dukku.common.shared.product.dto.shop.ShopProductListResponse;
import dukku.common.shared.product.dto.product.ProductListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FindMyShopProductsUseCase {

        private final ProductSellerRepository productSellerRepository;
        private final ProductRepository productRepository;

        @Transactional
        public ShopProductListResponse execute(UUID userUuid, SaleStatus saleStatus, int page, int size) {

                Pageable pageable = PageRequest.of(
                                Math.max(page, 0),
                                Math.min(size, 50),
                                Sort.by(Sort.Direction.DESC, "createdAt"));

                // 내 상점 조회: 만약 이벤트 유실이나 레거시 유저 등으로 인해 ProductSeller가 없다면 즉시 생성한다 (Defense in
                // Depth / Fallback)
                ProductSeller seller = productSellerRepository.findByUserUuid(userUuid)
                                .orElseGet(() -> productSellerRepository
                                                .save(ProductSeller.create(userUuid, "반가워요! 내 상점입니다.")));

                // seller.userUuid == Product.sellerUuid
                UUID sellerUserUuid = seller.getUserUuid();

                Page<Product> result = (saleStatus == null)
                                ? productRepository.findBySellerUuidAndDeletedAtIsNull(sellerUserUuid, pageable)
                                : productRepository.findBySellerUuidAndSaleStatusAndDeletedAtIsNull(sellerUserUuid,
                                                saleStatus, pageable);

                List<ProductListItemResponse> items = result.getContent().stream()
                                .map(ProductMapper::toListItem)
                                .toList();

                return ShopProductListResponse.from(result, items);
        }
}
