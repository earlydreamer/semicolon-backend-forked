package dukku.product.global;

import dukku.product.boundedContext.product.app.cqrs.ReindexAllProductsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "product.reindex-on-startup", havingValue = "true")
public class ProductReindexRunner implements CommandLineRunner {
    private final ReindexAllProductsUseCase reindexAllProductsUseCase;

    @Override
    public void run(String... args) {
        log.info("[ReindexRunner] product.reindex-on-startup=true 감지. 재색인을 수행합니다.");
        reindexAllProductsUseCase.execute();
    }
}
