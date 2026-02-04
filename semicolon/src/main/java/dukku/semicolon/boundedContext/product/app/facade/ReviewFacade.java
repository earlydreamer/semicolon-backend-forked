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
    private final FindSellerReviewSummaryUseCase findSellerReviewSummaryUseCase;

    public SellerReviewResponse createSellerReview(UUID userUuid, @Valid SellerReviewCreateRequest request) {
        return createSellerReviewUseCase.execute(userUuid, request);
    }

    public SellerReviewResponse updateSellerReview(UUID userUuid, UUID reviewUuid, @Valid SellerReviewUpdateRequest request) {
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
