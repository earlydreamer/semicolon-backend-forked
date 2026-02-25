package dukku.product.global;

import dukku.product.boundedContext.product.out.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
@ConditionalOnProperty(name = "product.clear-es-on-startup", havingValue = "true")
public class ElasticsearchClearRunner implements CommandLineRunner {

    private final ProductSearchRepository productSearchRepository;

    @Override
    public void run(String... args) {
        log.info("[ElasticsearchClearRunner] product.clear-es-on-startup=true 감지. ES 인덱스를 초기화합니다.");
        try {
            productSearchRepository.deleteAll();
            log.info("[ElasticsearchClearRunner] ES 인덱스 초기화 완료.");
        } catch (Exception e) {
            log.warn("[ElasticsearchClearRunner] ES 인덱스 초기화 실패: {}", e.getMessage());
        }
    }
}
