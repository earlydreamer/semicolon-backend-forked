package dukku.common.shared.product.dto.review;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class SellerReviewSummaryResponse {
    private UUID sellerUuid;
    private double avgRating;
    private long reviewCount;
}
