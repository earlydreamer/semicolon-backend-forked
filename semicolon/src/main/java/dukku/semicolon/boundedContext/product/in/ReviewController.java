package dukku.semicolon.boundedContext.product.in;

import dukku.common.global.UserUtil;
import dukku.semicolon.boundedContext.product.app.facade.ReviewFacade;
import dukku.semicolon.shared.product.docs.ReviewApiDocs;
import dukku.semicolon.shared.product.dto.review.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@ReviewApiDocs.ReviewTag
public class ReviewController {

    private final ReviewFacade reviewFacade;

    @PostMapping("/seller-reviews")
    @ReviewApiDocs.CreateSellerReview
    public SellerReviewResponse createProductReview(
            @RequestBody @Valid SellerReviewCreateRequest request
    ) {
        return reviewFacade.createProductReview(UserUtil.getUserId(), request);
    }

    @PatchMapping("/seller-reviews/{reviewUuid}")
    @ReviewApiDocs.UpdateSellerReview
    public SellerReviewResponse updateProductReview(
            @PathVariable UUID reviewUuid,
            @RequestBody @Valid SellerReviewUpdateRequest request
    ) {
        return reviewFacade.updateProductReview(UserUtil.getUserId(), reviewUuid, request);
    }

    @DeleteMapping("/seller-reviews/{reviewUuid}")
    @ReviewApiDocs.DeleteSellerReview
    public void deleteProductReview(
            @PathVariable UUID reviewUuid
    ) {
        reviewFacade.deleteProductReview(UserUtil.getUserId(), reviewUuid);
    }

    @GetMapping("/sellers/{sellerUuid}/reviews-summary")
    @ReviewApiDocs.FindSellerReviewSummary
    public SellerReviewSummaryResponse findSellerReviewSummary(
            @PathVariable UUID sellerUuid
    ) {
        return reviewFacade.findSellerReviewSummary(sellerUuid);
    }

    // 최신 리뷰가 먼저 오도록 정렬
    @GetMapping("/sellers/{sellerUuid}/reviews")
    @ReviewApiDocs.FindAllSellerReviews
    public SellerReviewListResponse findSellerAllReviews(
            @PathVariable UUID sellerUuid,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return reviewFacade.findSellerAllReviews(sellerUuid, pageable);
    }
}
