package dukku.semicolon.boundedContext.product.app.usecase.review;

import dukku.semicolon.boundedContext.product.app.support.SellerReviewMapper;
import dukku.semicolon.boundedContext.product.entity.SellerReview;
import dukku.semicolon.boundedContext.product.out.SellerReviewRepository;
import dukku.semicolon.shared.product.dto.review.SellerReviewResponse;
import dukku.semicolon.shared.product.dto.review.SellerReviewUpdateRequest;
import dukku.semicolon.shared.product.exception.ReviewNotFoundException;
import dukku.semicolon.shared.product.exception.ReviewUnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateSellerReviewUseCase {

    private final SellerReviewRepository sellerReviewRepository;

    @Transactional
    public SellerReviewResponse execute(UUID buyerUuid, UUID reviewUuid, SellerReviewUpdateRequest request) {

        SellerReview review = sellerReviewRepository.findByUuidAndDeletedAtIsNull(reviewUuid)
                .orElseThrow(ReviewNotFoundException::new);

        // 작성자 검증
        if (!review.getBuyerUuid().equals(buyerUuid)) {
            throw new ReviewUnauthorizedException();
        }

        // 부분 수정
        review.changeRating(request.getRating());
        review.changeContent(request.getContent());

        return SellerReviewMapper.toResponse(review);
    }
}
