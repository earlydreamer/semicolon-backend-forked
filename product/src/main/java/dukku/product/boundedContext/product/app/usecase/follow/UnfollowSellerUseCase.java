package dukku.product.boundedContext.product.app.usecase.follow;

import dukku.common.shared.product.dto.follow.FollowActionResponse;
import dukku.common.shared.product.exception.ProductSellerNotFoundException;
import dukku.common.shared.product.exception.SelfFollowNotAllowedException;
import dukku.product.boundedContext.product.entity.ProductSeller;
import dukku.product.boundedContext.product.out.ProductSellerRepository;
import dukku.product.boundedContext.product.out.SellerFollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UnfollowSellerUseCase {

    private final SellerFollowRepository sellerFollowRepository;
    private final ProductSellerRepository productSellerRepository;

    @Transactional
    public FollowActionResponse execute(UUID userUuid, UUID sellerUuid) {
        ProductSeller seller = productSellerRepository.findBySellerUuid(sellerUuid)
                .orElseThrow(ProductSellerNotFoundException::new);

        if (userUuid.equals(seller.getUserUuid())) {
            throw new SelfFollowNotAllowedException();
        }

        // 멱등: 없으면 그냥 unfollowed 응답
        if (!sellerFollowRepository.existsByUserUuidAndSellerUuid(userUuid, sellerUuid)) {
            return FollowActionResponse.unfollowed(sellerUuid);
        }

        sellerFollowRepository.deleteByUserUuidAndSellerUuid(userUuid, sellerUuid);
        return FollowActionResponse.unfollowed(sellerUuid);
    }
}
