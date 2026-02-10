package dukku.common.shared.product.event;

import java.util.Set;

public record ReviewStatsSyncedEvent(Set<String> sellerUuids) {
}
