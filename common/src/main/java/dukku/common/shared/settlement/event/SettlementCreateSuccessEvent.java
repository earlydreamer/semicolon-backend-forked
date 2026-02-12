package dukku.common.shared.settlement.event;

import dukku.common.global.event.DomainEvent;
import java.util.UUID;

public record SettlementCreateSuccessEvent() implements DomainEvent {
    @Override
    public String getTopic() {
        return "settlement.create.success";
    }

    @Override
    public String getKey() {
        return UUID.randomUUID().toString();
    }
}
