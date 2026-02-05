package dukku.semicolon.boundedContext.product.app.cqrs;

import dukku.semicolon.boundedContext.product.entity.ProductSeller;
import dukku.semicolon.boundedContext.product.out.ProductSellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReviewStatsSyncUseCase {

    private final SellerReviewStatsRedisSupport sellerReviewStatsRedisSupport;
    private final ProductSellerRepository productSellerRepository;

    @Transactional
    public void execute() {

        Set<Object> dirty = sellerReviewStatsRedisSupport.getDirtySellerUuids();
        if (dirty == null || dirty.isEmpty()) return;

        // UUID 리스트 변환
        List<UUID> sellerUuids = dirty.stream()
                .map(o -> UUID.fromString(o.toString()))
                .toList();

        // sellers 한 번에 조회
        List<ProductSeller> sellers =
                productSellerRepository.findBySellerUuidIn(sellerUuids);

        // 메모리에서 값 계산 & 세팅
        for (ProductSeller seller : sellers) {

            UUID sellerUuid = seller.getSellerUuid();

            long countLong = sellerReviewStatsRedisSupport.getReviewCount(sellerUuid);
            long ratingSum = sellerReviewStatsRedisSupport.getRatingSum(sellerUuid);

            int count = (int) Math.max(0, Math.min(countLong, Integer.MAX_VALUE));

            BigDecimal avg = BigDecimal.ZERO;
            if (count > 0) {
                avg = BigDecimal.valueOf(ratingSum)
                        .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            }

            seller.updateReviewSummary(count, avg);
        }

        // dirty cleanup
        sellerReviewStatsRedisSupport.cleanupDirty(dirty);
    }
}
