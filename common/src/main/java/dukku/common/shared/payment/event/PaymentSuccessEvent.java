package dukku.common.shared.payment.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 결제 완료 이벤트
 * <p>
 * 결제가 성공적으로 완료되었을 때 발행되며,
 * 이후 예치금 차감(Deposit BC) 및 주문 상태 변경(Order BC)의 트리거가 됩니다.
 */
import dukku.common.global.event.DomainEvent;

public record PaymentSuccessEvent(
        UUID paymentUuid,
        UUID orderUuid,
        Long amount,
        Long pgAmount, // pg 결제 금액
        Long paymentDeposit, // 사용된 예치금
        UUID userUuid,
        LocalDateTime occurredAt,
        List<ItemDepositUsage> itemDepositUsages,
        UUID couponUuid) implements DomainEvent {

    public PaymentSuccessEvent(
            UUID paymentUuid,
            UUID orderUuid,
            Long amount,
            Long pgAmount,
            Long paymentDeposit,
            UUID userUuid,
            LocalDateTime occurredAt,
            List<ItemDepositUsage> itemDepositUsages) {
        this(paymentUuid, orderUuid, amount, pgAmount, paymentDeposit, userUuid, occurredAt, itemDepositUsages, null);
    }

    @Override
    public String getTopic() {
        return "payment.success";
    }

    @Override
    public String getKey() {
        return orderUuid.toString();
    }
        /**
         * 상품별 예치금 사용 상세 내역
         */
        public record ItemDepositUsage(
                        UUID orderItemUuid,
                        Long depositAmount) {
        }
}
