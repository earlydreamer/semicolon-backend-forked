package dukku.product.boundedContext.product.app.usecase.shop;

import dukku.common.shared.product.dto.shop.ShopResponse;
import dukku.common.shared.user.dto.UserProfileResponse;
import dukku.common.shared.user.out.UserApiClient;
import dukku.product.boundedContext.product.entity.ProductSeller;
import dukku.product.boundedContext.product.entity.ProductUser;
import dukku.product.boundedContext.product.out.ProductSellerRepository;
import dukku.product.boundedContext.product.out.ProductUserRepository;
import dukku.product.boundedContext.product.out.SellerReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FindMyShopUseCase {

    private final ProductSellerRepository productSellerRepository;
    private final ProductUserRepository productUserRepository;
    private final SellerReviewRepository sellerReviewRepository;
    private final UserApiClient userApiClient;

    @Transactional
    public ShopResponse execute(UUID userUuid) {
        // 내 상점 조회: 이벤트 지연/누락으로 ProductSeller가 없으면 즉시 생성
        ProductSeller seller = productSellerRepository.findByUserUuid(userUuid)
                .orElseGet(() -> productSellerRepository.save(ProductSeller.create(userUuid, "반가워요! 내 상점입니다.")));

        String nickname = resolveNicknameWithBackfill(seller.getUserUuid());
        long reviewCountLong = sellerReviewRepository.countBySellerUuidAndDeletedAtIsNull(seller.getSellerUuid());
        int reviewCount = Math.toIntExact(reviewCountLong);
        BigDecimal averageRating = BigDecimal.valueOf(sellerReviewRepository.avgRating(seller.getSellerUuid()))
                .setScale(2, RoundingMode.HALF_UP);

        return ShopResponse.builder()
                .shopUuid(seller.getUuid())
                .sellerUuid(seller.getSellerUuid())
                .nickname(nickname)
                .intro(seller.getIntro())
                .salesCount(seller.getSalesCount())
                .activeListingCount(seller.getActiveListingCount())
                .averageRating(averageRating)
                .reviewCount(reviewCount)
                .build();
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
                    log.warn("[FindMyShopUseCase] ProductUser 보정 저장 실패. userUuid={}", userUuid, e);
                }
            }

            return nickname;
        } catch (Exception e) {
            log.warn("[FindMyShopUseCase] 유저 프로필 조회 실패로 기본 닉네임 사용. userUuid={}", userUuid, e);
            return "이름없음";
        }
    }
}
