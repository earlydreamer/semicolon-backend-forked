package dukku.product.boundedContext.product.app.usecase.shop;

import dukku.common.shared.product.dto.shop.ShopResponse;
import dukku.common.shared.product.dto.shop.UpdateShopRequest;
import dukku.common.shared.product.exception.ProductSellerNotFoundException;
import dukku.common.shared.user.dto.UserProfileResponse;
import dukku.common.shared.user.out.UserApiClient;
import dukku.product.boundedContext.product.entity.ProductSeller;
import dukku.product.boundedContext.product.entity.ProductUser;
import dukku.product.boundedContext.product.out.ProductSellerRepository;
import dukku.product.boundedContext.product.out.ProductUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateMyShopUseCase {

    private final ProductSellerRepository productSellerRepository;
    private final ProductUserRepository productUserRepository;
    private final UserApiClient userApiClient;

    @Transactional
    public ShopResponse execute(UUID userUuid, UpdateShopRequest request) {
        ProductSeller seller = productSellerRepository.findByUserUuid(userUuid)
                .orElseThrow(ProductSellerNotFoundException::new);

        // intro만 수정 (null이면 그대로 유지)
        if (request.getIntro() != null) {
            seller.changeIntro(request.getIntro());
        }

        String nickname = resolveNicknameWithBackfill(seller.getUserUuid());

        return ProductSeller.from(seller, nickname);
    }

    private String resolveNicknameWithBackfill(UUID userUuid) {
        return productUserRepository.findById(userUuid)
                .map(ProductUser::getNickname)
                .filter(StringUtils::hasText)
                .orElseGet(() -> fetchAndBackfillNickname(userUuid));
    }

    private String fetchAndBackfillNickname(UUID userUuid) {
        try {
            UserProfileResponse profile = userApiClient.getUserProfile(userUuid);
            String nickname = profile == null ? null : profile.getNickname();
            if (!StringUtils.hasText(nickname)) {
                return "이름없음";
            }

            if (!productUserRepository.existsById(userUuid)) {
                try {
                    productUserRepository.save(ProductUser.create(userUuid, nickname));
                } catch (Exception e) {
                    log.warn("[UpdateMyShopUseCase] ProductUser 보정 저장 실패. userUuid={}", userUuid, e);
                }
            }

            return nickname;
        } catch (Exception e) {
            log.warn("[UpdateMyShopUseCase] 유저 프로필 조회 실패로 기본 닉네임 사용. userUuid={}", userUuid, e);
            return "이름없음";
        }
    }
}
