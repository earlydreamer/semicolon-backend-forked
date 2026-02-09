package dukku.semicolon.shared.product.dto.follow;

import java.math.BigDecimal;
import java.util.UUID;

public record FollowedSellerCardResponse(
        UUID sellerUuid,
        String nickname,
        String intro,
        BigDecimal averageRating,
        int reviewCount,
        long followerCount,
        boolean followed
) {}
