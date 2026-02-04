package dukku.semicolon.boundedContext.product.app.cqrs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReviewStatsSyncFacade {

    private final ReviewStatsSyncUseCase reviewStatsSyncUseCase;

    @Transactional
    public void syncAllStats() {
        reviewStatsSyncUseCase.execute();
    }
}
