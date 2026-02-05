package dukku.semicolon.boundedContext.product.app.cqrs;

import dukku.semicolon.boundedContext.product.entity.ProductSeller;
import dukku.semicolon.boundedContext.product.out.ProductSellerRepository;
import dukku.semicolon.shared.product.event.ReviewStatsSyncedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReviewStatsSyncUseCase {

    private final SellerReviewStatsRedisSupport redisSupport;
    private final ProductSellerRepository productSellerRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void execute() {

        Set<String> dirty = redisSupport.getDirtySellerUuids();
        if (dirty == null || dirty.isEmpty()) return;

        // UUID 리스트 변환
        List<UUID> sellerUuids = dirty.stream()
                .map(UUID::fromString)
                .toList();

        // sellers 한 번에 조회
        List<ProductSeller> sellers =
                productSellerRepository.findBySellerUuidIn(sellerUuids);

        // 메모리에서 값 계산 & 세팅
        for (ProductSeller seller : sellers) {

            UUID sellerUuid = seller.getSellerUuid();

            long countLong = redisSupport.getReviewCount(sellerUuid);
            long ratingSum = redisSupport.getRatingSum(sellerUuid);

            int count = (int) Math.max(0, Math.min(countLong, Integer.MAX_VALUE));

            BigDecimal avg = BigDecimal.ZERO;
            if (count > 0) {
                avg = BigDecimal.valueOf(ratingSum)
                        .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            }

            seller.updateReviewSummary(count, avg);
        }

        productSellerRepository.saveAll(sellers);

        // 실제로 처리된 seller만 cleanup 대상으로 보냄
        Set<String> processed = sellers.stream()
                .map(s -> s.getSellerUuid().toString())
                .collect(Collectors.toSet());

        eventPublisher.publishEvent(new ReviewStatsSyncedEvent(processed));
    }
}
