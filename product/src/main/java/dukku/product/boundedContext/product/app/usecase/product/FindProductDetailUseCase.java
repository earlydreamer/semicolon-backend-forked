package dukku.product.boundedContext.product.app.usecase.product;

import dukku.product.boundedContext.product.app.support.ProductMapper;
import dukku.product.boundedContext.product.app.cqrs.ProductStatsRedisSupport;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.entity.ProductSeller;
import dukku.product.boundedContext.product.out.ProductRepository;
import dukku.product.boundedContext.product.out.ProductSellerRepository;
import dukku.product.boundedContext.product.out.ProductUserRepository;
import dukku.common.shared.product.dto.product.ProductDetailResponse;
import dukku.common.shared.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

        @Transactional
        public ProductDetailResponse execute(UUID productUuid) {
                log.info("[FindProductDetailUseCase] 상품 상세 조회 시작. productUuid={}", productUuid);

                Product product = productRepository.findByUuidWithImagesAndCategory(productUuid)
                                .or(() -> {
                                        log.warn("[FindProductDetailUseCase] 페치 조인 쿼리로 상품 조회 실패. 기본 조회(findByUuid)를 시도합니다. productUuid={}",
                                                        productUuid);
                                        return productRepository.findByUuid(productUuid);
                                })
                                .orElseThrow(() -> {
                                        log.error("[FindProductDetailUseCase] 어떤 방식으로도 상품을 찾을 수 없습니다. productUuid={}",
                                                        productUuid);
                                        return new ProductNotFoundException();
                                });

                log.info("[FindProductDetailUseCase] 상품 조회 성공. title={}, sellerUuid={}", product.getTitle(),
                                product.getSellerUuid());

                productStatsRedisSupport.incrementView(product.getId());

                // 상점 정보 조회: 만약 이벤트 유실이나 레거시 유저 등으로 인해 ProductSeller가 없다면 즉시 생성한다 (데이터 정합성 복구 /
                // Fallback)
                ProductSeller seller = productSellerRepository.findByUserUuid(product.getSellerUuid())
                                .orElseGet(() -> {
                                        log.warn("[FindProductDetailUseCase] 상점 정보(ProductSeller)가 존재하지 않아 자동 생성합니다. userUuid={}, productUuid={}",
                                                        product.getSellerUuid(), productUuid);
                                        return productSellerRepository.save(ProductSeller
                                                        .create(product.getSellerUuid(), "반가워요! 내 상점입니다."));
                                });

                log.info("[FindProductDetailUseCase] 상점 정보 확보 성공. sellerUuid={}", seller.getUserUuid());

                String nickname = productUserRepository.findById(seller.getUserUuid())
                                .map(user -> user.getNickname())
                                .filter(StringUtils::hasText)
                                .orElseGet(() -> {
                                        log.warn("[FindProductDetailUseCase] 유저 정보(ProductUser)가 존재하지 않아 폴백 닉네임을 사용합니다. userUuid={}",
                                                        seller.getUserUuid());
                                        // 유저 정보가 없더라도 상세 페이지는 보여주기 위해 임시 닉네임 반환 (필요시 DB에 저장할수도 있음)
                                        return "이름없음";
                                });

                log.info("[FindProductDetailUseCase] 최종 조회 완료. nickname={}", nickname);

                return ProductMapper.toDetail(product, seller, nickname);
        }
}
