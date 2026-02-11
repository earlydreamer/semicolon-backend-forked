package dukku.common.shared.product.dto.review;

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
}
