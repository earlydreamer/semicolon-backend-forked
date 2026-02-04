package dukku.semicolon.boundedContext.product.app.cqrs;

import dukku.semicolon.boundedContext.product.out.ProductSellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        for (Object id : dirty) {
            UUID sellerUuid = UUID.fromString(id.toString());

            long countLong = sellerReviewStatsRedisSupport.getReviewCount(sellerUuid);
            long ratingSum = sellerReviewStatsRedisSupport.getRatingSum(sellerUuid);

            int count = (int) Math.max(0, Math.min(countLong, Integer.MAX_VALUE));

            BigDecimal avg = BigDecimal.ZERO;
            if (count > 0) {
                avg = BigDecimal.valueOf(ratingSum)
                        .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP); // 소수 2자리
            }

            productSellerRepository.updateReviewSummary(sellerUuid, count, avg);
        }

        sellerReviewStatsRedisSupport.cleanupDirty(dirty);
    }
}
