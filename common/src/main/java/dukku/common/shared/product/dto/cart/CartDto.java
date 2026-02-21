package dukku.common.shared.product.dto.cart;

import dukku.common.shared.product.type.SaleStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CartDto(
        int cartId,
        UUID productUuid,
        UUID sellerUuid,
        String title,
        long price,
        SaleStatus saleStatus,
        String thumbnailUrl,
        LocalDateTime createdAt
) {
}
