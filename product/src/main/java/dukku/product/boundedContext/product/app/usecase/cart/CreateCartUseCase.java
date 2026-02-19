package dukku.product.boundedContext.product.app.usecase.cart;

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

@Service
@RequiredArgsConstructor
@Transactional
public class CreateCartUseCase {
    private static final int UUID_PREFIX_LENGTH = 8;

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductUserRepository productUserRepository;

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
