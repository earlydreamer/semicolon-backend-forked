package dukku.product.boundedContext.product.app.usecase.review;

import dukku.product.boundedContext.product.out.SellerReviewRepository;
import dukku.common.shared.product.dto.review.SellerReviewSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FindSellerReviewSummaryUseCase {

    private final SellerReviewRepository sellerReviewRepository;

    @Transactional(readOnly = true)
    public SellerReviewSummaryResponse execute(UUID sellerUuid) {
        double avg = sellerReviewRepository.avgRating(sellerUuid);
        long count = sellerReviewRepository.countBySellerUuidAndDeletedAtIsNull(sellerUuid);

        return SellerReviewSummaryResponse.builder()
                .sellerUuid(sellerUuid)
                .avgRating(avg)
                .reviewCount(count)
                .build();
    }
}
