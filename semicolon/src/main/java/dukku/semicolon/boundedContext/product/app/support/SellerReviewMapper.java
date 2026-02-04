package dukku.semicolon.boundedContext.product.app.support;

import dukku.semicolon.boundedContext.product.entity.SellerReview;
import dukku.semicolon.shared.product.dto.review.SellerReviewResponse;

public class SellerReviewMapper {

    public static SellerReviewResponse toResponse(SellerReview r) {
        return SellerReviewResponse.builder()
                .reviewUuid(r.getUuid())
                .sellerUuid(r.getSellerUuid())
                .buyerUuid(r.getBuyerUuid())
                .orderItemUuid(r.getOrderItemUuid())
                .productUuid(r.getProductUuid())
                .rating(r.getRating())
                .content(r.getContent())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
