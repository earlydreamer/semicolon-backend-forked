package dukku.semicolon.boundedContext.product.app.cqrs;

import dukku.semicolon.boundedContext.product.out.ProductSellerRepository;
import dukku.semicolon.shared.product.dto.cqrs.SellerReviewStatDto;
import dukku.semicolon.shared.product.event.ReviewStatsSyncedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

        List<SellerReviewStatDto> items = dirty.stream()
                .map(UUID::fromString)
                .map(sellerUuid -> {
                    long countLong = redisSupport.getReviewCount(sellerUuid);
                    long ratingSum = redisSupport.getRatingSum(sellerUuid);

                    int count = (int) Math.max(0, Math.min(countLong, Integer.MAX_VALUE));

                    BigDecimal avg = BigDecimal.ZERO;
                    if (count > 0) {
                        avg = BigDecimal.valueOf(ratingSum)
                                .divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);
                    }

                    return new SellerReviewStatDto(sellerUuid, count, avg);
                })
                .toList();

        // DB Bulk Update (즉시 UPDATE)
        productSellerRepository.bulkUpdateReviewSummary(items);

        // AFTER_COMMIT cleanup 이벤트는 기존처럼 processed만 발행
        Set<String> processed = items.stream()
                .map(i -> i.sellerUuid().toString())
                .collect(java.util.stream.Collectors.toSet());
        eventPublisher.publishEvent(new ReviewStatsSyncedEvent(processed));
    }
}
