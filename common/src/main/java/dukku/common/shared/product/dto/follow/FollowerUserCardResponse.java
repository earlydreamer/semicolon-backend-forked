package dukku.common.shared.product.dto.follow;

import java.util.UUID;

public record FollowerUserCardResponse(
        UUID userUuid,
        String nickname
) {
}
