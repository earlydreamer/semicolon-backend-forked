package dukku.product.boundedContext.product.app.usecase.shop;

import dukku.common.shared.product.dto.shop.ShopResponse;
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
public class FindMyShopUseCase {

    private final ProductSellerRepository productSellerRepository;
    private final ProductUserRepository productUserRepository;

    @Transactional
    public ShopResponse execute(UUID userUuid) {
        // 내 상점 조회: 만약 이벤트 유실이나 레거시 유저 등으로 인해 ProductSeller가 없다면 즉시 생성한다 (Defense in
        // Depth / Fallback)
        ProductSeller seller = productSellerRepository.findByUserUuid(userUuid)
                .orElseGet(() -> productSellerRepository.save(ProductSeller.create(userUuid, "반가워요! 내 상점입니다.")));

        String nickname = productUserRepository.findById(seller.getUserUuid())
                .map(user -> user.getNickname())
                .filter(StringUtils::hasText)
                .orElseGet(() -> "이름없음");

        return ProductSeller.from(seller, nickname);
    }
}
