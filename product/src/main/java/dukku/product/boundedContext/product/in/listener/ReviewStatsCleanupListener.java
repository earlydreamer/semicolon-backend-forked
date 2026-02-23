package dukku.product.boundedContext.product.in.listener;

import dukku.product.boundedContext.product.app.cqrs.review.ReviewStatsRedisSupport;
import dukku.common.shared.product.event.ReviewStatsSyncedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReviewStatsCleanupListener {

    private final ReviewStatsRedisSupport redisSupport;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ReviewStatsSyncedEvent event) {
        redisSupport.cleanupDirty(event.sellerUuids());
    }
}
