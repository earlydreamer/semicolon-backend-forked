package dukku.semicolon.boundedContext.product.app.usecase.follow;

import dukku.semicolon.boundedContext.product.entity.SellerFollow;
import dukku.semicolon.boundedContext.product.out.ProductSellerRepository;
import dukku.semicolon.boundedContext.product.out.SellerFollowRepository;
import dukku.semicolon.shared.product.dto.follow.FollowSellerResponse;
import dukku.semicolon.shared.product.exception.ProductSellerNotFoundException;
import dukku.semicolon.shared.product.exception.SelfFollowNotAllowedException;
import dukku.semicolon.shared.product.exception.SellerAlreadyFollowedException;
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
    public FollowSellerResponse execute(UUID userUuid, UUID sellerUuid) {

        if (userUuid.equals(sellerUuid)) {
                throw new SelfFollowNotAllowedException();
        }

        // 판매자(상점) 존재 검증: sellerUuid는 "그 사람 userUuid"로 들어온다고 가정
        productSellerRepository.findByUserUuid(sellerUuid)
                .orElseThrow(ProductSellerNotFoundException::new);

        if (sellerFollowRepository.existsByUserUuidAndSellerUuid(userUuid, sellerUuid)) {
            throw new SellerAlreadyFollowedException();
        }

        sellerFollowRepository.save(SellerFollow.create(userUuid, sellerUuid));

        return FollowSellerResponse.followed(sellerUuid, true);
    }
}
