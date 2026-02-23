package dukku.product.boundedContext.product.app.usecase.product;

import dukku.product.boundedContext.product.app.support.ProductMapper;
import dukku.product.boundedContext.product.app.cqrs.ProductStatsRedisSupport;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.out.ProductRepository;
import dukku.common.shared.product.dto.product.ProductDetailResponse;
import dukku.common.shared.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindProductDetailUseCase {
    private final ProductRepository productRepository;
    private final ProductStatsRedisSupport productStatsRedisSupport;

    public ProductDetailResponse execute(UUID productUuid) {
        Product product = productRepository.findByUuidWithImagesAndCategory(productUuid)
                .orElseThrow(ProductNotFoundException::new);

        productStatsRedisSupport.incrementView(product.getId());

        return ProductMapper.toDetail(product);
    }
}
