package dukku.common.shared.product.dto.cart;

import dukku.common.shared.product.type.CartEventType;

import java.time.LocalDateTime;
import java.util.UUID;

public record CartPayload (
        CartEventType eventType,
        UUID userUuid,
        UUID productUuid,
        String productTitle,
        Integer categoryId,
        LocalDateTime timestamp
){}
