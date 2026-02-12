package dukku.common.shared.product.event;

import dukku.common.global.event.DomainEvent;
import java.util.Set;
import java.util.UUID;

public record ReviewStatsSyncedEvent(Set<String> sellerUuids) implements DomainEvent {
    @Override
    public String getTopic() {
        return "product.review.stats-synced";
    }

    @Override
    public String getKey() {
        return "bulk-sync-" + UUID.randomUUID();
    }
}
