package dukku.product.boundedContext.product.app.cqrs;

import dukku.product.boundedContext.product.app.cqrs.review.ReviewStatsSyncFacade;
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
            log.warn("상품 통계 동기화 실패", e);
        }

        try {
            reviewStatsSyncFacade.syncAllStats();
        } catch (Exception e) {
            log.warn("리뷰 통계 동기화 실패", e);
        }
    }
}
