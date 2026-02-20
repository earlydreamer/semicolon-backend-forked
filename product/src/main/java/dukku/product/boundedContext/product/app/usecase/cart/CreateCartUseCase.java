package dukku.product.boundedContext.product.app.usecase.cart;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.global.exception.BadRequestException;
import dukku.common.global.exception.ConflictException;
import dukku.common.global.exception.NotFoundException;
import dukku.common.shared.product.dto.cart.CartPayload;
import dukku.common.shared.product.event.CartSyncEvent;
import dukku.common.shared.product.type.AccountStatus;
import dukku.common.shared.product.type.CartEventType;
import dukku.common.shared.product.exception.CartProductAlreadyExistsException;
import dukku.common.shared.product.exception.CartSelfProductNotAllowedException;
import dukku.common.shared.product.exception.CartSoldOutProductNotAllowedException;
import dukku.common.shared.product.exception.ProductNotFoundException;
import dukku.common.shared.product.type.SaleStatus;
import dukku.product.boundedContext.product.entity.Cart;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.entity.ProductUser;
import dukku.product.boundedContext.product.out.CartRepository;
import dukku.product.boundedContext.product.out.ProductRepository;
import dukku.product.boundedContext.product.out.ProductUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static dukku.product.boundedContext.product.entity.Cart.toCartPayload;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateCartUseCase {
    private static final CartEventType CART_EVENT_TYPE = CartEventType.ITEM_ADDED;
    private static final int UUID_PREFIX_LENGTH = 8;

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductUserRepository productUserRepository;
    private final EventPublisher eventPublisher;

    public void execute(UUID userUuid, UUID productUuid) {
        Product product = productRepository.findByUuid(productUuid)
                .orElseThrow(ProductNotFoundException::new);

        ProductUser user = productUserRepository
                .findById(userUuid)
                .orElseGet(() ->
                        productUserRepository.save(
                                ProductUser.create(userUuid, "user-" + userUuid.toString().substring(0, UUID_PREFIX_LENGTH))
                        )
                );

        validateCart(user, product);

        if (cartRepository.existsByUserAndProduct(user, product)) {
            throw new CartProductAlreadyExistsException();
        }

        Cart cart = Cart.createCart(user, product);
        cartRepository.save(cart);

        CartPayload payload = toCartPayload(cart, CART_EVENT_TYPE);
        // 트랜잭션 커밋된 후 이벤트 발행
        eventPublisher.publishAfterCommit(new CartSyncEvent(payload));
    }

    private void validateCart(ProductUser user, Product product) {
        if (product.getSellerUuid().equals(user.getUserUuid())) {
            throw new CartSelfProductNotAllowedException();
        }

        if (product.getSaleStatus() == SaleStatus.SOLD_OUT) {
            throw new CartSoldOutProductNotAllowedException();
        }
    }
}
