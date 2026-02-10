package dukku.common.shared.product.dto.follow;

import java.util.UUID;

public record FollowActionResponse(
        UUID sellerUuid,
        boolean followed
) {
    public static FollowActionResponse followed(UUID sellerUuid) {
        return new FollowActionResponse(sellerUuid, true);
    }

    public static FollowActionResponse unfollowed(UUID sellerUuid) {
        return new FollowActionResponse(sellerUuid, false);
    }
}
