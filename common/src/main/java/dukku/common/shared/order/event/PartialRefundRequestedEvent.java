package dukku.common.shared.order.event;

import dukku.common.global.event.DomainEvent;
import java.util.List;
import java.util.UUID;

public record PartialRefundRequestedEvent(
        UUID returnRequestUuid,
        UUID orderUuid,
        UUID userUuid,
        List<RefundItemInfo> refundItems) implements DomainEvent {

    @Override
    public String getTopic() {
        return "order.partial_refund_requested";
    }

    @Override
    public String getKey() {
        return orderUuid.toString();
    }

    public record RefundItemInfo(
            UUID orderItemUuid,
            int refundAmount) {
    }
}
