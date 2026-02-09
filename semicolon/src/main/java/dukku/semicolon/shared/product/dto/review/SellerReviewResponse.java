package dukku.semicolon.shared.product.dto.review;

import dukku.semicolon.boundedContext.product.entity.SellerReview;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class SellerReviewResponse {
    private UUID reviewUuid;
    private UUID sellerUuid;
    private UUID buyerUuid;
    private UUID orderItemUuid;
    private UUID productUuid;
    private int rating;
    private String content;
    private java.time.LocalDateTime createdAt;

    public static SellerReviewResponse from(SellerReview r) {
        return SellerReviewResponse.builder()
                .reviewUuid(r.getUuid())
                .sellerUuid(r.getSellerUuid())
                .buyerUuid(r.getBuyerUuid())
                .orderItemUuid(r.getOrderItemUuid())
                .productUuid(r.getProductUuid())
                .rating(r.getRating())
                .content(r.isDeleted() ? "삭제된 후기입니다." : r.getContent())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
