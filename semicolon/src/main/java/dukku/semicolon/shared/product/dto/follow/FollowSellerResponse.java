package dukku.semicolon.shared.product.dto.follow;

import java.util.UUID;

public record FollowSellerResponse(
        UUID sellerUuid,
        boolean followed,
        UUID followerUuid // followers 조회용이면 필요
) {
    public static FollowSellerResponse followed(UUID sellerUuid, boolean followed) {
        return new FollowSellerResponse(sellerUuid, followed, null);
    }

    public static FollowSellerResponse follower(UUID followerUuid) {
        return new FollowSellerResponse(null, false, followerUuid);
    }
}
