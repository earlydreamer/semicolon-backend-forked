package dukku.semicolon.boundedContext.product.app.usecase.review;

import dukku.semicolon.boundedContext.product.app.cqrs.review.ReviewStatsRedisSupport;
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
    private final ReviewStatsRedisSupport reviewStatsRedisSupport;

    @Transactional
    public SellerReviewResponse execute(UUID buyerUuid, UUID reviewUuid, SellerReviewUpdateRequest request) {

        SellerReview review = sellerReviewRepository.findByUuidAndDeletedAtIsNull(reviewUuid)
                .orElseThrow(ReviewNotFoundException::new);

        // 작성자 검증
        if (!review.getBuyerUuid().equals(buyerUuid)) {
            throw new ReviewUnauthorizedException();
        }

        UUID sellerUuid = review.getSellerUuid();

        int beforeRating = review.getRating();
        int newRating = request.getRating();

        // (1) 도메인 수정
        review.changeContent(request.getContent());

        // rating이 들어오는 정책이면 아래처럼 처리
        if (beforeRating != newRating) {
            review.changeRating(newRating);

            // (2) Redis 통계 반영
            reviewStatsRedisSupport.subtractRating(sellerUuid, beforeRating);
            reviewStatsRedisSupport.addRating(sellerUuid, newRating);
        }

        return SellerReviewMapper.toResponse(review);
    }
}
