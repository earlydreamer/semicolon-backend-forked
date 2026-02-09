package dukku.semicolon.shared.product.dto.follow;

import java.util.UUID;

public record FollowedSellerCardResponse(
        UUID sellerUuid,
        String nickname,
        String intro,
//        BigDecimal averageRating,
        int followerCount,
        boolean followed
) {}
