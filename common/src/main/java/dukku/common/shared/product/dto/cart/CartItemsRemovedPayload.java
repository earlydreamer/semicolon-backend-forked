package dukku.common.shared.product.dto.cart;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CartItemsRemovedPayload(
        UUID userUuid,
        List<UUID> productUuids,
        boolean allCleared,
        LocalDateTime timestamp
) implements CartEventPayload {
    @Override
    public String getUserUuid() {
        return userUuid.toString();
    }
}
