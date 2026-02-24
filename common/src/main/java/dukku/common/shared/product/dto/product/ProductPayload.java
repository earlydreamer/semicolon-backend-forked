package dukku.common.shared.product.dto.product;

import dukku.common.shared.product.type.ProductEventType;
import dukku.common.shared.product.type.SaleStatus;

import java.util.List;
import java.util.UUID;

public record ProductPayload (
        ProductEventType eventType,
        UUID productUuid,
        String title,
        String description,
        Long price,
        Long shippingFee,
        Integer categoryId,
        String categoryName,
        List<String> tags,
        SaleStatus saleStatus,
        String productUrl
) {
}
