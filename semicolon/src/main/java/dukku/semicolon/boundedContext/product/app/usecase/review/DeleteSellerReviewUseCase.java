package dukku.semicolon.boundedContext.product.app.usecase.review;

import dukku.semicolon.boundedContext.product.entity.SellerReview;
import dukku.semicolon.boundedContext.product.out.SellerReviewRepository;
import dukku.semicolon.shared.product.exception.ReviewNotFoundException;
import dukku.semicolon.shared.product.exception.ReviewUnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeleteSellerReviewUseCase {

    private final SellerReviewRepository sellerReviewRepository;

    @Transactional
    public void execute(UUID buyerUuid, UUID reviewUuid) {

        SellerReview review = sellerReviewRepository.findByUuid(reviewUuid)
                .orElseThrow(ReviewNotFoundException::new);

        if(!review.getBuyerUuid().equals(buyerUuid)) {
            throw new ReviewUnauthorizedException();
        }

        // 이미 삭제면 그대로 종료 (idempotent) : soft delete 고려
        if (review.isDeleted()) {
            return;
        }

        review.softDelete();
    }
}
