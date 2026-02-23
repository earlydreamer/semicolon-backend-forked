package dukku.product.boundedContext.product.app.usecase.shop;

import dukku.common.shared.product.dto.shop.ShopResponse;
import dukku.common.shared.product.dto.shop.UpdateShopRequest;
import dukku.common.shared.product.exception.ProductSellerNotFoundException;
import dukku.common.shared.product.exception.ProductUserNotFoundException;
import dukku.product.boundedContext.product.entity.ProductSeller;
import dukku.product.boundedContext.product.out.ProductSellerRepository;
import dukku.product.boundedContext.product.out.ProductUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateMyShopUseCase {

    private final ProductSellerRepository productSellerRepository;
    private final ProductUserRepository productUserRepository;

    @Transactional
    public ShopResponse execute(UUID userUuid, UpdateShopRequest request) {
        ProductSeller seller = productSellerRepository.findByUserUuid(userUuid)
                .orElseThrow(ProductSellerNotFoundException::new);

        // intro만 수정 (null이면 그대로 유지)
        if (request.getIntro() != null) {
            seller.changeIntro(request.getIntro());
        }

        String nickname = productUserRepository.findById(seller.getUserUuid())
                .map(user -> user.getNickname())
                .filter(StringUtils::hasText)
                .orElseThrow(ProductUserNotFoundException::new);

        return ProductSeller.from(seller, nickname);
    }
}
