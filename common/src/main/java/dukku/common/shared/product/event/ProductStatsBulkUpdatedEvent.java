package dukku.common.shared.product.event;

import dukku.common.shared.product.dto.cqrs.ProductStatDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

import dukku.common.global.event.DomainEvent;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ProductStatsBulkUpdatedEvent implements DomainEvent {
    private List<ProductStatDto> stats;

    @Override
    public String getTopic() {
        return "product.stats-updated";
    }

    @Override
    public String getKey() {
        return UUID.randomUUID().toString();
    }
}
