package dukku.semicolon.boundedContext.product.app.facade;

import dukku.semicolon.boundedContext.product.app.usecase.review.*;
import dukku.semicolon.shared.product.dto.review.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReviewFacade {

    private final CreateSellerReviewUseCase createSellerReviewUseCase;
    private final UpdateSellerReviewUseCase updateSellerReviewUseCase;
    private final DeleteSellerReviewUseCase deleteSellerReviewUseCase;
    private final FindSellerReviewListUseCase findSellerReviewListUseCase;
    private final FindSellerRatingUseCase findSellerRatingUseCase;

    public SellerReviewResponse createProductReview(UUID userUuid, @Valid SellerReviewCreateRequest request) {
        return createSellerReviewUseCase.execute(userUuid, request);
    }

    public SellerReviewResponse updateProductReview(UUID userUuid, UUID reviewUuid, @Valid SellerReviewUpdateRequest request) {
        return updateSellerReviewUseCase.execute(userUuid, reviewUuid, request);
    }

    public void deleteProductReview(UUID userUuid, UUID reviewUuid) {
        deleteSellerReviewUseCase.execute(userUuid, reviewUuid);
    }

    public SellerRatingResponse findSellerRating(UUID sellerUuid) {
        return findSellerRatingUseCase.execute(sellerUuid);
    }

    public SellerReviewListResponse findSellerAllReviews(UUID sellerUuid, Pageable pageable) {
        return findSellerReviewListUseCase.execute(sellerUuid, pageable);
    }
}
