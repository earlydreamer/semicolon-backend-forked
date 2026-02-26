package dukku.product.boundedContext.product.app.usecase.follow;

import dukku.common.shared.product.dto.follow.FollowActionResponse;
import dukku.common.shared.product.exception.ProductSellerNotFoundException;
import dukku.common.shared.product.exception.SelfFollowNotAllowedException;
import dukku.common.shared.product.exception.SellerAlreadyFollowedException;
import dukku.product.boundedContext.product.entity.ProductSeller;
import dukku.product.boundedContext.product.entity.SellerFollow;
import dukku.product.boundedContext.product.out.ProductSellerRepository;
import dukku.product.boundedContext.product.out.SellerFollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FollowSellerUseCase {

    private final SellerFollowRepository sellerFollowRepository;
    private final ProductSellerRepository productSellerRepository;

    @Transactional
    public FollowActionResponse execute(UUID userUuid, UUID sellerUuid) {
        ProductSeller seller = productSellerRepository.findBySellerUuid(sellerUuid)
                .orElseThrow(ProductSellerNotFoundException::new);

        if (userUuid.equals(seller.getUserUuid())) {
            throw new SelfFollowNotAllowedException();
        }

        if (sellerFollowRepository.existsByUserUuidAndSellerUuid(userUuid, sellerUuid)) {
            throw new SellerAlreadyFollowedException();
        }

        sellerFollowRepository.save(SellerFollow.create(userUuid, sellerUuid));
        return FollowActionResponse.followed(sellerUuid);
    }
}
