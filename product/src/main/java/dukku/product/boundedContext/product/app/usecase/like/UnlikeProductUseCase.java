package dukku.product.boundedContext.product.app.usecase.like;

import dukku.product.boundedContext.product.app.cqrs.ProductStatsRedisSupport;
import dukku.product.boundedContext.product.out.ProductLikeRepository;
import dukku.product.boundedContext.product.out.ProductRepository;
import dukku.common.shared.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UnlikeProductUseCase {
    private final ProductRepository productRepository;
    private final ProductLikeRepository productLikeRepository;
    private final ProductStatsRedisSupport productStatsRedisSupport;

    @Transactional
    public void execute(UUID userUuid, UUID productUuid) {
        int productId = productRepository.findIdByUuidAndDeletedAtIsNull(productUuid)
                .orElseThrow(ProductNotFoundException::new);

        int deleted = productLikeRepository
                .deleteByUserUuidAndProductUuid(userUuid, productUuid);

        if (deleted == 1) {
            productStatsRedisSupport.decrementLike(productId);
        }
    }
}
