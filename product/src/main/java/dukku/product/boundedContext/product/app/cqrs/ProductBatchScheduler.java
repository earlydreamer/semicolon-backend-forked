package dukku.semicolon.boundedContext.product.app.cqrs;

import dukku.semicolon.boundedContext.product.app.cqrs.review.ReviewStatsSyncFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductBatchScheduler {
    private final ProductSyncFacade productSyncFacade;
    private final ReviewStatsSyncFacade reviewStatsSyncFacade;

    @Scheduled(fixedRate = 60000) // 1분
    public void scheduleSync() {
        try {
            productSyncFacade.syncAllStats();
        } catch (Exception e) {
            log.warn("product stats sync failed", e);
        }

        try {
            reviewStatsSyncFacade.syncAllStats();
        } catch (Exception e) {
            log.warn("review stats sync failed", e);
        }
    }
}
