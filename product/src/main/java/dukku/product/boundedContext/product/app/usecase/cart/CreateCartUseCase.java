package dukku.product.boundedContext.product.app.usecase.cart;

import dukku.common.global.exception.BadRequestException;
import dukku.common.global.exception.ConflictException;
import dukku.common.global.exception.NotFoundException;
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
                .orElseThrow(() -> new NotFoundException("존재하지 않는 상품입니다."));

        ProductUser user = productUserRepository
                .findById(userUuid)
                .orElseGet(() ->
                        productUserRepository.save(
                                ProductUser.create(userUuid, "user-" + userUuid.toString().substring(0, UUID_PREFIX_LENGTH))
                        )
                );

        validateCart(user, product);

        if (cartRepository.existsByUserAndProduct(user, product)) {
            throw new ConflictException("이미 장바구니에 담긴 상품입니다.");
        }

        Cart cart = Cart.createCart(user, product);
        cartRepository.save(cart);
    }

    private void validateCart(ProductUser user, Product product) {
        if (product.getSellerUuid().equals(user.getUserUuid())) {
            throw new BadRequestException("자신의 상품은 담을 수 없습니다.");
        }

        if (product.getSaleStatus() == SaleStatus.SOLD_OUT) {
            throw new BadRequestException("판매 완료된 상품은 담을 수 없습니다.");
        }
    }
}
