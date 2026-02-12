package dukku.common.shared.product.dto.cqrs;

import java.math.BigDecimal;
import java.util.UUID;

public record SellerReviewStatDto(
        UUID sellerUuid,
        int reviewCount,
        BigDecimal averageRating
) {
}
