package dukku.semicolon.boundedContext.product.app.usecase.follow;

import dukku.semicolon.boundedContext.product.out.SellerFollowRepository;
import dukku.semicolon.shared.product.dto.follow.FollowSellerResponse;
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
    public List<FollowSellerResponse> execute(UUID userUuid) {
        return sellerFollowRepository.findByUserUuid(userUuid).stream()
                .map(f -> FollowSellerResponse.followed(f.getSellerUuid(), true))
                .toList();
    }
}
