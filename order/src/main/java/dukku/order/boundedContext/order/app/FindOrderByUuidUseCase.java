package dukku.order.boundedContext.order.app;

import dukku.order.boundedContext.order.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FindOrderByUuidUseCase {

    private final OrderSupport orderSupport;

    @Transactional(readOnly = true)
    public Order execute(UUID orderUuid) {
        return orderSupport.findOrderByUuidWithItems(orderUuid);
    }
}
