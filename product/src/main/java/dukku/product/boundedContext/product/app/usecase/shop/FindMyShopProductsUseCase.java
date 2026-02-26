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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FindMyShopProductsUseCase {

        private final ProductSellerRepository productSellerRepository;
        private final ProductRepository productRepository;

        @Transactional(readOnly = true)
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

                UUID sellerUuid = seller.getSellerUuid();
                UUID sellerUserUuid = seller.getUserUuid();

                Page<Product> result = (saleStatus == null)
                                ? productRepository.findBySellerUuidAndDeletedAtIsNull(sellerUuid, pageable)
                                : productRepository.findBySellerUuidAndSaleStatusAndDeletedAtIsNull(sellerUuid,
                                                saleStatus, pageable);

                // 레거시 호환: 과거 데이터 중 product.sellerUuid=userUuid로 저장된 경우 fallback
                if (result.isEmpty() && !sellerUuid.equals(sellerUserUuid)) {
                        result = (saleStatus == null)
                                        ? productRepository.findBySellerUuidAndDeletedAtIsNull(sellerUserUuid, pageable)
                                        : productRepository.findBySellerUuidAndSaleStatusAndDeletedAtIsNull(sellerUserUuid,
                                                        saleStatus, pageable);
                }

                Map<Integer, List<String>> tagNamesByProductId = buildTagMap(result.getContent());

                List<ProductListItemResponse> items = result.getContent().stream()
                                .map(product -> ProductMapper.toListItem(
                                                product,
                                                tagNamesByProductId.getOrDefault(product.getId(), Collections.emptyList())))
                                .toList();

                return ShopProductListResponse.from(result, items);
        }

        private Map<Integer, List<String>> buildTagMap(List<Product> products) {
                if (products.isEmpty()) {
                        return Collections.emptyMap();
                }

                List<Integer> productIds = products.stream()
                                .map(Product::getId)
                                .toList();

                Map<Integer, List<String>> tagsByProductId = new HashMap<>();
                for (Object[] row : productRepository.findTagNamesByProductIds(productIds)) {
                        Integer productId = (Integer) row[0];
                        String tagName = (String) row[1];
                        tagsByProductId.computeIfAbsent(productId, ignored -> new ArrayList<>()).add(tagName);
                }
                return tagsByProductId;
        }
}
