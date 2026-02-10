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
public record PaymentSuccessEvent(
                UUID paymentUuid, // 2026-01-24 추가
                UUID paymentId, // 2026-01-24 추가
                UUID orderUuid,
                Long amount,
                Long pgAmount, // pg 결제 금액
                Long paymentDeposit, // 사용된 예치금
                UUID userUuid,
                LocalDateTime occurredAt,
                List<ItemDepositUsage> itemDepositUsages) { // 2026-01-24 추가

        /**
         * 이 이벤트가 발행되는 Kafka 토픽명.
         * Producer(EventPublisher)와 Consumer(@KafkaListener)가 동일한 상수를 참조하여
         * 토픽명 불일치를 컴파일 타임에 방지한다.
         */
        public static final String TOPIC = "payment.success";

        /**
         * 상품별 예치금 사용 상세 내역
         */
        public record ItemDepositUsage(
                        UUID orderItemUuid,
                        Long depositAmount) {
        }
}
