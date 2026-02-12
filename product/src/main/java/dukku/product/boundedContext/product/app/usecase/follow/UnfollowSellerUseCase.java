package dukku.product.boundedContext.product.app.usecase.follow;

import dukku.product.boundedContext.product.out.SellerFollowRepository;
import dukku.common.shared.product.dto.follow.FollowActionResponse;
import dukku.common.shared.product.exception.SelfFollowNotAllowedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UnfollowSellerUseCase {

    private final SellerFollowRepository sellerFollowRepository;

    @Transactional
    public FollowActionResponse execute(UUID userUuid, UUID sellerUuid) {

        if (userUuid.equals(sellerUuid)) {
            throw new SelfFollowNotAllowedException();
        }

        // 멱등: 없으면 그냥 "unfollowed"로 응답
        if (!sellerFollowRepository.existsByUserUuidAndSellerUuid(userUuid, sellerUuid)) {
            return FollowActionResponse.unfollowed(sellerUuid);
        }

        sellerFollowRepository.deleteByUserUuidAndSellerUuid(userUuid, sellerUuid);
        return FollowActionResponse.unfollowed(sellerUuid);
    }
}
