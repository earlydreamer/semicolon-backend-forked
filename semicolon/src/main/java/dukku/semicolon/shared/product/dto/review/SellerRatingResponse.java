package dukku.semicolon.shared.product.dto.review;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class SellerRatingResponse {
    private UUID sellerUuid;
    private double avgRating;
    private long reviewCount;
}
