package dukku.product.boundedContext.product.app.usecase.product;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.out.ProductRepository;
import dukku.product.global.event.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmProductSaleUseCase {
    private final ProductRepository productRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void execute(UUID orderUuid, List<UUID> productUuids) {
        List<Product> products = productRepository.findAllByUuidIn(productUuids);

        for (Product product : products) {
            // 1. DB 상태 변경 (Dirty Checking)
            product.confirmSale(orderUuid);

            // 2. ES 동기화 트리거 (이벤트 발행)
            // TODO: N+1으로 병목 발생 가능성 있음. 추후 부하테스트 시 확인 필요
            eventPublisher.publish(new ProductUpdatedEvent(product.getId(), false));
        }

        log.info("판매 확정 처리 완료: 제품들={}", productUuids);
    }
}
