package dukku.product.boundedContext.product.app.usecase.cart;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.global.exception.NotFoundException;
import dukku.common.shared.product.dto.cart.CartItemsRemovedPayload;
import dukku.common.shared.product.event.CartSyncEvent;
import dukku.common.shared.product.type.CartEventType;
import dukku.product.boundedContext.product.entity.Cart;
import dukku.product.boundedContext.product.out.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional
public class DeleteCartUseCase {
    private static final CartEventType CART_EVENT_TYPE = CartEventType.ITEM_REMOVED;

    private final CartRepository cartRepository;
    private final EventPublisher eventPublisher;

    public void execute(UUID userUuid, int cartId) {
        execute(userUuid, List.of(cartId));
    }

    public void execute(UUID userUuid, List<Integer> cartIds) {
        List<Cart> carts = cartRepository.findAllByIdInAndUser_UserUuid(cartIds, userUuid);
        if (carts == null || carts.isEmpty()) {
            throw new NotFoundException("삭제할 장바구니 항목을 찾을 수 없습니다.");
        }

        List<UUID> productUuids = carts.stream()
                .map(c -> c.getProduct().getUuid())
                .collect(Collectors.toList());

        cartRepository.deleteAll(carts);

        // 삭제 이벤트 페이로드: 소비자는 삭제 대상 식별을 위해 반드시 productUuids를 사용하도록 권장합니다.
        // allCleared는 false (선택 삭제)
        CartItemsRemovedPayload payload = new CartItemsRemovedPayload(
                userUuid,
                productUuids,
                false,
                LocalDateTime.now()
        );

        eventPublisher.publishAfterCommit(new CartSyncEvent(CART_EVENT_TYPE, payload));
    }
}
