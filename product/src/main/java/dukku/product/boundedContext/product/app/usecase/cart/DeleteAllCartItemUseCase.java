package dukku.product.boundedContext.product.app.usecase.cart;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.product.dto.cart.CartItemsRemovedPayload;
import dukku.common.shared.product.event.CartSyncEvent;
import dukku.common.shared.product.type.CartEventType;
import dukku.product.boundedContext.product.entity.Cart;
import dukku.product.boundedContext.product.out.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteAllCartItemUseCase {
    private static final CartEventType CART_EVENT_TYPE = CartEventType.CART_CLEARED;

    private final CartRepository cartRepository;
    private final EventPublisher eventPublisher;

    public void execute(UUID userUuid){
        List<Cart> items = cartRepository.findAllWithProductByUserUuid(userUuid);
        List<UUID> productUuids = items.stream()
                .map(c -> c.getProduct().getUuid())
                .collect(Collectors.toList());

        cartRepository.deleteByUser_UserUuid(userUuid);

        CartItemsRemovedPayload payload = new CartItemsRemovedPayload(
                userUuid,
                productUuids,
                true,
                LocalDateTime.now()
        );

        eventPublisher.publishAfterCommit(new CartSyncEvent(CART_EVENT_TYPE, payload));
    }
}
