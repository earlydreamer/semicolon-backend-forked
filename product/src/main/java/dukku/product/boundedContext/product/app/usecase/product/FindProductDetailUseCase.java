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

    @Transactional
    public ProductDetailResponse execute(UUID productUuid) {
        log.info("[FindProductDetailUseCase] 상품 상세 조회 시작. productUuid={}", productUuid);

        Product product = productRepository.findByUuidWithImagesAndCategory(productUuid)
                .or(() -> {
                    log.warn("[FindProductDetailUseCase] 패치 조인 조회 실패, 기본 조회로 재시도. productUuid={}", productUuid);
                    return productRepository.findByUuid(productUuid);
                })
                .orElseThrow(() -> {
                    log.error("[FindProductDetailUseCase] 상품을 찾을 수 없습니다. productUuid={}", productUuid);
                    return new ProductNotFoundException();
                });

        log.info("[FindProductDetailUseCase] 상품 조회 성공. title={}, sellerUuid={}", product.getTitle(), product.getSellerUuid());

        productStatsRedisSupport.incrementView(product.getId());

        // Product.sellerUuid는 seller UUID이므로 sellerUuid 기준으로 먼저 조회한다.
        ProductSeller seller = productSellerRepository.findBySellerUuid(product.getSellerUuid())
                // 과거 데이터 호환: sellerUuid에 userUuid가 들어간 레코드도 허용
                .or(() -> productSellerRepository.findByUserUuid(product.getSellerUuid()))
                .orElseGet(() -> {
                    // 상세 조회(GET)에서 DB write를 유발하지 않도록 비영속 기본값만 사용한다.
                    log.warn("[FindProductDetailUseCase] 상점 정보 누락. 기본값으로 응답합니다. sellerUuid={}, productUuid={}",
                            product.getSellerUuid(), productUuid);
                    return ProductSeller.create(product.getSellerUuid(), "판매 중인 상점입니다.");
                });

        log.info("[FindProductDetailUseCase] 상점 정보 조회 성공. sellerUserUuid={}", seller.getUserUuid());

        String nickname = resolveNicknameWithBackfill(seller.getUserUuid());
        long reviewCountLong = sellerReviewRepository.countBySellerUuidAndDeletedAtIsNull(seller.getSellerUuid());
        int reviewCount = Math.toIntExact(reviewCountLong);
        BigDecimal averageRating = BigDecimal.valueOf(sellerReviewRepository.avgRating(seller.getSellerUuid()))
                .setScale(2, RoundingMode.HALF_UP);

        log.info("[FindProductDetailUseCase] 최종 조회 완료. nickname={}", nickname);

        return ProductMapper.toDetail(product, seller, nickname, averageRating, reviewCount);
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
                    log.warn("[FindProductDetailUseCase] ProductUser 보정 저장 실패. userUuid={}", userUuid, e);
                }
            }

            return nickname;
        } catch (Exception e) {
            log.warn("[FindProductDetailUseCase] 사용자 프로필 조회 실패로 기본 닉네임 사용. userUuid={}", userUuid, e);
            return "이름없음";
        }
    }
}
