package dukku.semicolon.boundedContext.product.app.usecase.review;

import dukku.semicolon.boundedContext.product.out.SellerReviewRepository;
import dukku.semicolon.shared.product.dto.review.SellerRatingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FindSellerRatingUseCase {

    private final SellerReviewRepository sellerReviewRepository;

    @Transactional(readOnly = true)
    public SellerRatingResponse execute(UUID sellerUuid) {
        double avg = sellerReviewRepository.avgRating(sellerUuid);
        long count = sellerReviewRepository.countBySellerUuidAndDeletedAtIsNull(sellerUuid);

        return SellerRatingResponse.builder()
                .sellerUuid(sellerUuid)
                .avgRating(avg)
                .reviewCount(count)
                .build();
    }
}
