package dukku.product.boundedContext.product.app.usecase.cart;

import dukku.product.boundedContext.product.out.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteAllCartItemUseCase {
    // private static final CartEventType CART_EVENT_TYPE = CartEventType.CART_CLEARED;

    private final CartRepository cartRepository;
    // private final EventPublisher eventPublisher;

    public void execute(UUID userUuid){

        cartRepository.deleteByUser_UserUuid(userUuid);

        // NOTE: 전체 삭제 이벤트 발행은 현재 불필요하다는 요청에 따라 임시로 비활성화했습니다.
        // 필요 시 아래 주석을 해제하면 전체 삭제 이벤트가 다시 발행됩니다.
        //
        // List<Cart> items = cartRepository.findAllWithProductByUserUuid(userUuid);
        // List<UUID> productUuids = items.stream()
        //         .map(c -> c.getProduct().getUuid())
        //         .collect(Collectors.toList());
        //
        // --- BEGIN commented out: cart clear event publishing ---
        // CartItemsRemovedPayload payload = new CartItemsRemovedPayload(
        //         userUuid,
        //         productUuids,
        //         true,
        //         LocalDateTime.now()
        // );
        // eventPublisher.publishAfterCommit(new CartSyncEvent(CART_EVENT_TYPE, payload));
        // --- END commented out ---
    }
}
