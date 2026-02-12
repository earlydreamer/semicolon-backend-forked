package dukku.common.shared.deposit.event;

import dukku.common.global.event.DomainEvent;
import java.util.UUID;

/**
 * 예치금 차감 성공 이벤트
 *
 * <p>
 * 예치금 사용(차감)이 정상적으로 완료되었을 때 발행.
 * Payment BC 등에서 후속 처리가 필요할 경우 사용.
 */
public record DepositUsedEvent(
        UUID orderUuid,
        UUID userUuid,
        Long amount) implements DomainEvent {
    @Override
    public String getTopic() {
        return "deposit.used";
    }

    @Override
    public String getKey() {
        return userUuid.toString();
    }
}
