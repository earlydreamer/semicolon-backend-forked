package dukku.common.shared.product.dto.cart;

public sealed interface CartEventPayload permits CartItemAddedPayload, CartItemsRemovedPayload {
    String getUserUuid();
}
