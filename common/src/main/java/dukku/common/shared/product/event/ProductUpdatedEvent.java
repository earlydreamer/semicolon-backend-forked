package dukku.common.shared.product.event;

import dukku.semicolon.boundedContext.product.entity.Product;

public record ProductUpdatedEvent(Product product, boolean isCategoryChanged) {
}
