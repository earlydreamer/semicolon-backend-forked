package dukku.common.shared.product.dto.follow;

import java.math.BigDecimal;
import java.util.UUID;

public record FollowedSellerCardResponse(
        UUID sellerUuid,
        UUID shopUuid,
        String nickname,
        String intro,
        BigDecimal averageRating,
        int reviewCount,
        long followerCount,
        boolean followed
) {
    public FollowedSellerCardResponse(
            UUID sellerUuid,
            UUID shopUuid,
            String nickname,
            String intro,
            Double averageRating,
            Long reviewCount,
            Long followerCount,
            Boolean followed
    ) {
        this(
                sellerUuid,
                shopUuid,
                nickname,
                intro,
                averageRating == null ? BigDecimal.ZERO : BigDecimal.valueOf(averageRating),
                reviewCount == null ? 0 : Math.toIntExact(reviewCount),
                followerCount == null ? 0L : followerCount,
                followed != null && followed
        );
    }
}
