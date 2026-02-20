package dukku.common.shared.product.dto.cart;

import java.time.LocalDateTime;
import java.util.UUID;

public record CartItemAddedPayload(
        UUID userUuid,
        UUID productUuid,
        String productTitle,
        Integer categoryId,
        LocalDateTime timestamp
) implements CartEventPayload {}
