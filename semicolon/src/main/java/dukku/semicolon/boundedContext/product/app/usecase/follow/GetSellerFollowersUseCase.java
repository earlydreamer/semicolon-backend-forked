package dukku.semicolon.boundedContext.product.app.usecase.follow;

import dukku.semicolon.boundedContext.product.out.ProductSellerRepository;
import dukku.semicolon.boundedContext.product.out.SellerFollowRepository;
import dukku.semicolon.shared.product.dto.follow.FollowSellerResponse;
import dukku.semicolon.shared.product.exception.ProductSellerNotFoundException;
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
    public List<FollowSellerResponse> execute(UUID sellerUuid) {
        // 판매자 존재 검증(없으면 404)
        productSellerRepository.findByUserUuid(sellerUuid)
                .orElseThrow(ProductSellerNotFoundException::new);

        return sellerFollowRepository.findBySellerUuid(sellerUuid).stream()
                .map(f -> FollowSellerResponse.follower(f.getUserUuid()))
                .toList();
    }
}
