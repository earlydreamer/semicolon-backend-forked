package dukku.semicolon.boundedContext.product.app.usecase.follow;

import dukku.semicolon.boundedContext.product.out.SellerFollowRepository;
import dukku.semicolon.shared.product.dto.follow.FollowedSellerCardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetFollowedSellersUseCase {

    private final SellerFollowRepository sellerFollowRepository;

    @Transactional(readOnly = true)
    public List<FollowedSellerCardResponse> execute(UUID userUuid) {

        return sellerFollowRepository.findFollowedSellerCards(userUuid);
    }
}
