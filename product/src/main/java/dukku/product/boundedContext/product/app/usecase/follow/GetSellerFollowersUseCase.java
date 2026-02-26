package dukku.product.boundedContext.product.app.usecase.follow;

import dukku.common.shared.product.dto.follow.FollowerUserCardResponse;
import dukku.common.shared.product.exception.ProductSellerNotFoundException;
import dukku.product.boundedContext.product.out.ProductSellerRepository;
import dukku.product.boundedContext.product.out.SellerFollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetSellerFollowersUseCase {

    private final SellerFollowRepository sellerFollowRepository;
    private final ProductSellerRepository productSellerRepository;

    @Transactional(readOnly = true)
    public List<FollowerUserCardResponse> execute(UUID sellerUuid) {
        productSellerRepository.findBySellerUuid(sellerUuid)
                .orElseThrow(ProductSellerNotFoundException::new);

        return sellerFollowRepository.findFollowerUserCards(sellerUuid);
    }
}
