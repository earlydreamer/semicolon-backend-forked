package dukku.product.boundedContext.product.app.usecase.cart;

import dukku.common.shared.product.dto.cart.CartDto;
import dukku.common.shared.product.dto.cart.CartListResponse;
import dukku.product.boundedContext.product.entity.Cart;
import dukku.product.boundedContext.product.out.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindMyCartListUseCase {
    private final CartRepository cartRepository;

    public CartListResponse execute(UUID userUuid) {
        // Fetch Join 쿼리로 조회 (Cart + Product + Images)
        List<Cart> carts = cartRepository.findAllWithProductByUserUuid(userUuid);

        List<CartDto> cartDtos = carts.stream()
                .map(Cart::toDto)
                .toList();

        int totalCount = cartDtos.size();

        long expectedTotalPrice = cartDtos.stream()
                .mapToLong(CartDto::price)
                .sum();

        return new CartListResponse(cartDtos, totalCount, expectedTotalPrice);
    }
}