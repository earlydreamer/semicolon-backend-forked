package dukku.product.boundedContext.product.app.usecase.shop;

import dukku.common.shared.product.dto.shop.ShopResponse;
import dukku.common.shared.product.exception.ProductSellerNotFoundException;
import dukku.product.boundedContext.product.entity.ProductSeller;
import dukku.product.boundedContext.product.out.ProductSellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FindShopUseCase {

    private final ProductSellerRepository productSellerRepository;

    @Transactional(readOnly = true)
    public ShopResponse execute(UUID shopUuid) {
        ProductSeller seller = productSellerRepository.findByUuid(shopUuid)
                .orElseThrow(ProductSellerNotFoundException::new);

        return ProductSeller.from(seller);
    }
}
