package dukku.product.boundedContext.product.app.cqrs.review;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReviewStatsSyncFacade {

    private final SyncReviewStatsUseCase syncReviewStatsUseCase;

    @Transactional
    public void syncAllStats() {
        syncReviewStatsUseCase.execute();
    }
}
