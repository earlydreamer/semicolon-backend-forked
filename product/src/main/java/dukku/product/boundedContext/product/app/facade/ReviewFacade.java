package dukku.product.boundedContext.product.app.facade;

import dukku.common.shared.product.dto.review.SellerReviewCreateRequest;
import dukku.common.shared.product.dto.review.SellerReviewListResponse;
import dukku.common.shared.product.dto.review.SellerReviewSummaryResponse;
import dukku.common.shared.product.dto.review.SellerReviewUpdateRequest;
import dukku.product.boundedContext.product.app.usecase.review.*;
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
    private final FindSellerReviewSummaryUseCase findSellerReviewSummaryUseCase;

    public dukku.common.shared.product.dto.review.SellerReviewResponse createSellerReview(UUID userUuid, @Valid SellerReviewCreateRequest request) {
        return createSellerReviewUseCase.execute(userUuid, request);
    }

    public dukku.common.shared.product.dto.review.SellerReviewResponse updateSellerReview(UUID userUuid, UUID reviewUuid, @Valid SellerReviewUpdateRequest request) {
        return updateSellerReviewUseCase.execute(userUuid, reviewUuid, request);
    }

    public void deleteSellerReview(UUID userUuid, UUID reviewUuid) {
        deleteSellerReviewUseCase.execute(userUuid, reviewUuid);
    }

    public SellerReviewSummaryResponse findSellerReviewSummary(UUID sellerUuid) {
        return findSellerReviewSummaryUseCase.execute(sellerUuid);
    }

    public SellerReviewListResponse findSellerReviewList(UUID sellerUuid, Pageable pageable) {
        return findSellerReviewListUseCase.execute(sellerUuid, pageable);
    }
}
