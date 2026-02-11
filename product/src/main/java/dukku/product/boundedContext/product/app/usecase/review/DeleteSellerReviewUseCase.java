package dukku.product.boundedContext.product.app.usecase.review;

import dukku.product.boundedContext.product.app.cqrs.review.ReviewStatsRedisSupport;
import dukku.product.boundedContext.product.entity.SellerReview;
import dukku.product.boundedContext.product.out.SellerReviewRepository;
import dukku.common.shared.product.exception.ReviewNotFoundException;
import dukku.common.shared.product.exception.ReviewUnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeleteSellerReviewUseCase {

    private final SellerReviewRepository sellerReviewRepository;
    private final ReviewStatsRedisSupport reviewStatsRedisSupport;

    @Transactional
    public void execute(UUID buyerUuid, UUID reviewUuid) {

        SellerReview review = sellerReviewRepository.findByUuid(reviewUuid)
                .orElseThrow(ReviewNotFoundException::new);

        if (!review.getBuyerUuid().equals(buyerUuid)) {
            throw new ReviewUnauthorizedException();
        }

        // 이미 삭제면 그대로 종료 (idempotent) : soft delete 고려
        if (review.isDeleted()) {
            return;
        }

        if (!review.isDeleted()) {
            reviewStatsRedisSupport.decrementReviewCount(review.getSellerUuid());
            reviewStatsRedisSupport.subtractRating(review.getSellerUuid(), review.getRating());
        }

        review.softDelete();
    }
}
