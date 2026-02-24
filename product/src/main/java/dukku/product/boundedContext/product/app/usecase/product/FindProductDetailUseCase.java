package dukku.product.boundedContext.product.app.usecase.product;

import dukku.product.boundedContext.product.app.support.ProductMapper;
import dukku.product.boundedContext.product.app.cqrs.ProductStatsRedisSupport;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.entity.ProductSeller;
import dukku.product.boundedContext.product.out.ProductRepository;
import dukku.product.boundedContext.product.out.ProductSellerRepository;
import dukku.product.boundedContext.product.out.ProductUserRepository;
import dukku.common.shared.product.dto.product.ProductDetailResponse;
import dukku.common.shared.product.exception.ProductNotFoundException;
import dukku.common.shared.product.exception.ProductSellerNotFoundException;
import dukku.common.shared.product.exception.ProductUserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindProductDetailUseCase {
    private final ProductRepository productRepository;
    private final ProductStatsRedisSupport productStatsRedisSupport;
    private final ProductSellerRepository productSellerRepository;
    private final ProductUserRepository productUserRepository;

    public ProductDetailResponse execute(UUID productUuid) {
        Product product = productRepository.findByUuidWithImagesAndCategory(productUuid)
                .orElseThrow(ProductNotFoundException::new);

        productStatsRedisSupport.incrementView(product.getId());

        ProductSeller seller = productSellerRepository.findByUserUuid(product.getSellerUuid())
                .orElseThrow(ProductSellerNotFoundException::new);

        String nickname = productUserRepository.findById(seller.getUserUuid())
                .map(user -> user.getNickname())
                .filter(StringUtils::hasText)
                .orElseThrow(ProductUserNotFoundException::new);

        return ProductMapper.toDetail(product, seller, nickname);
    }
}
