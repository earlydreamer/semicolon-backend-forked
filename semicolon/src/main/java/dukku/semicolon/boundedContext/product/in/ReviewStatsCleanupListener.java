package dukku.semicolon.boundedContext.product.in;

import dukku.semicolon.boundedContext.product.app.cqrs.SellerReviewStatsRedisSupport;
import dukku.semicolon.shared.product.event.ReviewStatsSyncedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReviewStatsCleanupListener {

    private final SellerReviewStatsRedisSupport redisSupport;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ReviewStatsSyncedEvent event) {
        redisSupport.cleanupDirty(event.sellerUuids());
    }
}
