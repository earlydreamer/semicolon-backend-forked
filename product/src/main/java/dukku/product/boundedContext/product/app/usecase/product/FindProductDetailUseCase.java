package dukku.product.boundedContext.product.app.usecase.product;

import dukku.common.shared.product.dto.product.ProductDetailResponse;
import dukku.common.shared.product.exception.ProductNotFoundException;
import dukku.common.shared.user.dto.UserProfileResponse;
import dukku.common.shared.user.out.UserApiClient;
import dukku.product.boundedContext.product.app.cqrs.ProductStatsRedisSupport;
import dukku.product.boundedContext.product.app.support.ProductMapper;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.entity.ProductSeller;
import dukku.product.boundedContext.product.entity.ProductUser;
import dukku.product.boundedContext.product.out.ProductRepository;
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
@Transactional(readOnly = true)
public class FindProductDetailUseCase {
    private final ProductRepository productRepository;
    private final ProductStatsRedisSupport productStatsRedisSupport;
    private final ProductSellerRepository productSellerRepository;
    private final ProductUserRepository productUserRepository;
    private final SellerReviewRepository sellerReviewRepository;
    private final UserApiClient userApiClient;

    // 상품 상세 조회 + 통계 캐시 반영 + 응답 DTO 조립
    @Transactional
    public ProductDetailResponse execute(UUID productUuid) {
        log.info("[FindProductDetailUseCase] Find product detail. productUuid={}", productUuid);

        Product product = productRepository.findByUuidWithImagesAndCategory(productUuid)
                .or(() -> {
                    log.warn("[FindProductDetailUseCase] fallback fetch by uuid. productUuid={}", productUuid);
                    return productRepository.findByUuid(productUuid);
                })
                .orElseThrow(() -> {
                    log.error("[FindProductDetailUseCase] product not found. productUuid={}", productUuid);
                    return new ProductNotFoundException();
                });

        log.info("[FindProductDetailUseCase] found product. title={}, sellerUuid={}", product.getTitle(), product.getSellerUuid());

        productStatsRedisSupport.incrementView(product.getId());
        Long redisViewCount = productStatsRedisSupport.getViewCount(product.getId());
        Long redisLikeCount = productStatsRedisSupport.getLikeCount(product.getId());

        ProductSeller seller = productSellerRepository.findBySellerUuid(product.getSellerUuid())
                .or(() -> productSellerRepository.findByUserUuid(product.getSellerUuid()))
                .orElseGet(() -> {
                    log.warn("[FindProductDetailUseCase] seller not found, fallback. sellerUuid={}, productUuid={}",
                            product.getSellerUuid(), productUuid);
                    return ProductSeller.create(product.getSellerUuid(), "default-nickname");
                });

        log.info("[FindProductDetailUseCase] seller found. sellerUserUuid={}", seller.getUserUuid());

        // 판매자 닉네임 조회 실패 시 사용자 서비스 조회로 보강
        String nickname = resolveNicknameWithBackfill(seller.getUserUuid());
        long reviewCountLong = sellerReviewRepository.countBySellerUuidAndDeletedAtIsNull(seller.getSellerUuid());
        int reviewCount = Math.toIntExact(reviewCountLong);
        BigDecimal averageRating = BigDecimal.valueOf(sellerReviewRepository.avgRating(seller.getSellerUuid()))
                .setScale(2, RoundingMode.HALF_UP);

        int resolvedLikeCount = redisLikeCount == null
                ? product.getLikeCount()
                : redisLikeCount.intValue();
        int resolvedViewCount = redisViewCount == null
                ? product.getViewCount()
                : redisViewCount.intValue();

        log.info("[FindProductDetailUseCase] completed. nickname={}", nickname);

        return ProductMapper.toDetail(
                product,
                seller,
                nickname,
                averageRating,
                reviewCount,
                resolvedLikeCount,
                resolvedViewCount
        );
    }

    // 로컬 사용자 테이블에 닉네임이 없으면 사용자 API로 보강 조회
    private String resolveNicknameWithBackfill(UUID userUuid) {
        return productUserRepository.findById(userUuid)
                .map(ProductUser::getNickname)
                .filter(StringUtils::hasText)
                .orElseGet(() -> fetchAndBackfillNickname(userUuid));
    }

    // 사용자 API에서 닉네임 조회, 실패 시 기본값 반환
    private String fetchAndBackfillNickname(UUID userUuid) {
        try {
            UserProfileResponse profile = userApiClient.getUserProfile(userUuid);
            String nickname = profile == null ? null : profile.getNickname();
            if (!StringUtils.hasText(nickname)) {
                return "anonymous";
            }

            if (!productUserRepository.existsById(userUuid)) {
                try {
                    productUserRepository.save(ProductUser.create(userUuid, nickname));
                } catch (Exception e) {
                    log.warn("[FindProductDetailUseCase] ProductUser save failed. userUuid={}", userUuid, e);
                }
            }

            return nickname;
        } catch (Exception e) {
            log.warn("[FindProductDetailUseCase] lookup user profile failed. userUuid={}", userUuid, e);
            return "anonymous";
        }
    }
}
