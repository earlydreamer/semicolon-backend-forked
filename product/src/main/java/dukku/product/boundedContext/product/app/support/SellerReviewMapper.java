package dukku.product.boundedContext.product.app.support;

import dukku.product.boundedContext.product.entity.SellerReview;
import dukku.common.shared.product.dto.review.SellerReviewResponse;

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
