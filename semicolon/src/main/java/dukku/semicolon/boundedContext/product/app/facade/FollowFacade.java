package dukku.semicolon.boundedContext.product.app.facade;

import dukku.semicolon.boundedContext.product.app.usecase.follow.FollowSellerUseCase;
import dukku.semicolon.boundedContext.product.app.usecase.follow.GetFollowedSellersUseCase;
import dukku.semicolon.boundedContext.product.app.usecase.follow.GetSellerFollowersUseCase;
import dukku.semicolon.boundedContext.product.app.usecase.follow.UnfollowSellerUseCase;
import dukku.semicolon.shared.product.dto.follow.FollowSellerResponse;
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

    public FollowSellerResponse followSeller(UUID userUuid, UUID sellerUuid) {
        return followSellerUseCase.execute(userUuid, sellerUuid);
    }

    public void unfollowSeller(UUID userUuid, UUID sellerUuid) {
        unfollowSellerUseCase.execute(userUuid, sellerUuid);
    }

    public List<FollowSellerResponse> getFollowedSellers(UUID userUuid) {
        return getFollowedSellersUseCase.execute(userUuid);
    }

    public List<FollowSellerResponse> getSellerFollowers(UUID sellerUuid) {
        return getSellerFollowersUseCase.execute(sellerUuid);
    }
}
