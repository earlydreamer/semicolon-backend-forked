package dukku.semicolon.boundedContext.product.app.facade;

import dukku.semicolon.boundedContext.product.app.usecase.follow.FollowSellerUseCase;
import dukku.semicolon.boundedContext.product.app.usecase.follow.GetFollowedSellersUseCase;
import dukku.semicolon.boundedContext.product.app.usecase.follow.GetSellerFollowersUseCase;
import dukku.semicolon.boundedContext.product.app.usecase.follow.UnfollowSellerUseCase;
import dukku.common.shared.product.dto.follow.FollowActionResponse;
import dukku.common.shared.product.dto.follow.FollowedSellerCardResponse;
import dukku.common.shared.product.dto.follow.FollowerUserCardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FollowFacade {

    private final FollowSellerUseCase followSellerUseCase;
    private final UnfollowSellerUseCase unfollowSellerUseCase;
    private final GetFollowedSellersUseCase getFollowedSellersUseCase;
    private final GetSellerFollowersUseCase getSellerFollowersUseCase;

    public FollowActionResponse followSeller(UUID userUuid, UUID sellerUuid) {
        return followSellerUseCase.execute(userUuid, sellerUuid);
    }

    public FollowActionResponse unfollowSeller(UUID userUuid, UUID sellerUuid) {
        return unfollowSellerUseCase.execute(userUuid, sellerUuid);
    }

    public List<FollowedSellerCardResponse> getFollowedSellers(UUID userUuid) {
        return getFollowedSellersUseCase.execute(userUuid);
    }

    public List<FollowerUserCardResponse> getSellerFollowers(UUID sellerUuid) {
        return getSellerFollowersUseCase.execute(sellerUuid);
    }
}
