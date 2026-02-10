package dukku.common.shared.deposit.event;

import dukku.common.shared.deposit.type.DepositFailureCode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 예치금 차감 실패 이벤트
 *
 * <p>
 * 예치금 잔액 부족 등의 이유로 차감이 실패했을 때 발행.
 * Payment BC는 이 이벤트를 수신하여 보상 트랜잭션(결제 취소)을 수행해야 함.
 * paymentUuid는 결제 식별자(레거시 발행자는 null 가능).
 * failureCode/retryable은 재시도 및 운영 판단에 사용.
 */
public record DepositDeductionFailedEvent(
        UUID orderUuid,
        UUID paymentUuid,
        UUID userUuid,
        Long amount,
        DepositFailureCode failureCode,
        boolean retryable,
        String reason,
        LocalDateTime occurredAt) {

    /**
     * 이 이벤트가 발행되는 Kafka 토픽명.
     * Producer(EventPublisher)와 Consumer(@KafkaListener)가 동일한 상수를 참조하여
     * 토픽명 불일치를 컴파일 타임에 방지한다.
     */
    public static final String TOPIC = "deposit.deduction-failed";

    public DepositDeductionFailedEvent(UUID orderUuid, UUID userUuid, Long amount, String reason) {
        this(orderUuid, null, userUuid, amount, DepositFailureCode.UNKNOWN, true, reason, LocalDateTime.now());
    }

    public DepositDeductionFailedEvent {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
