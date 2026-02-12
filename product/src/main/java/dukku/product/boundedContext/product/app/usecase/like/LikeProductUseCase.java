package dukku.product.boundedContext.product.app.usecase.like;

import dukku.product.boundedContext.product.app.cqrs.ProductStatsRedisSupport;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.entity.ProductLike;
import dukku.product.boundedContext.product.out.ProductLikeRepository;
import dukku.product.boundedContext.product.out.ProductRepository;
import dukku.common.shared.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LikeProductUseCase {
    private final ProductRepository productRepository;
    private final ProductLikeRepository productLikeRepository;
    private final ProductStatsRedisSupport productStatsRedisSupport;

    @Transactional
    public void execute(UUID userUuid, UUID productUuid) {
        Product product = productRepository.findByUuidAndDeletedAtIsNull(productUuid)
                .orElseThrow(ProductNotFoundException::new);

        if (!productLikeRepository.existsByUserUuidAndProduct_Uuid(userUuid, productUuid)) {
            ProductLike like = productLikeRepository.save(ProductLike.create(userUuid, product));
            productStatsRedisSupport.incrementLike(like.getProduct().getId());
        }
    }
}
