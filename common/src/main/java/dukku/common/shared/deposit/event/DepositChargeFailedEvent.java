package dukku.common.shared.deposit.event;

import dukku.common.global.event.DomainEvent;
import java.util.UUID;

// 예치금 충전 실패 이벤트
public record DepositChargeFailedEvent(
                UUID userUuid,
                Long amount,
                UUID settlementUuid,
                String reason) implements DomainEvent {
    @Override
    public String getTopic() {
        return "deposit.charge.failed";
    }

    @Override
    public String getKey() {
        return userUuid.toString();
    }
}
