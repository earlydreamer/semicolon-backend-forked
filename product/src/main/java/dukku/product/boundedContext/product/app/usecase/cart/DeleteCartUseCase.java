package dukku.product.boundedContext.product.app.usecase.cart;

import dukku.common.shared.product.exception.CartItemNotFoundException;
import dukku.product.boundedContext.product.entity.Cart;
import dukku.product.boundedContext.product.out.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class DeleteCartUseCase {
    // private static final CartEventType CART_EVENT_TYPE = CartEventType.ITEM_REMOVED;

    private final CartRepository cartRepository;
    // private final EventPublisher eventPublisher;

    public void execute(UUID userUuid, int cartId) {
        execute(userUuid, List.of(cartId));
    }

    public void execute(UUID userUuid, List<Integer> cartIds) {
        List<Cart> carts = cartRepository.findAllByIdInAndUser_UserUuid(cartIds, userUuid);
        if (carts == null || carts.isEmpty()) {
            throw new CartItemNotFoundException();
        }

        cartRepository.deleteAll(carts);

        // NOTE: 삭제 이벤트 발행은 현재 불필요하다는 요청에 따라 임시로 비활성화했습니다.
        // 필요 시 아래 주석을 해제하면 삭제 이벤트가 다시 발행됩니다.
        // List<UUID> productUuids = carts.stream()
        //        .map(c -> c.getProduct().getUuid())
        //        .collect(Collectors.toList());
        //
        // --- BEGIN commented out: deletion event publishing ---
        // CartItemsRemovedPayload payload = new CartItemsRemovedPayload(
        //         userUuid,
        //         productUuids,
        //         false,
        //         LocalDateTime.now()
        // );
        // eventPublisher.publishAfterCommit(new CartSyncEvent(CART_EVENT_TYPE, payload));
        // --- END commented out ---
    }
}
