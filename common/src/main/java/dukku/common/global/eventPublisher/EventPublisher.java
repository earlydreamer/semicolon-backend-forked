package dukku.common.global.eventPublisher;

import dukku.common.shared.deposit.event.DepositChargeFailedEvent;
import dukku.common.shared.deposit.event.DepositChargeSucceededEvent;
import dukku.common.shared.deposit.event.DepositDeductionFailedEvent;
import dukku.common.shared.deposit.event.DepositRefundedEvent;
import dukku.common.shared.deposit.event.DepositUsedEvent;
import dukku.common.shared.payment.event.PaymentCompensationFailedEvent;
import dukku.common.shared.payment.event.PaymentFailedEvent;
import dukku.common.shared.payment.event.PaymentSuccessEvent;
import dukku.common.shared.payment.event.RefundCompletedEvent;
import dukku.common.shared.payment.event.RefundFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

/**
 * 이벤트 발행기 (Spring Event + Kafka 동시 발행)
 *
 * <p>
 * Phase A: Spring Event(하위 호환) + Kafka(Payment/Deposit 도메인용) 동시 발행.
 * DB 트랜잭션 커밋 후 Kafka 발행을 보장하기 위해 {@code TransactionSynchronization.afterCommit()}을 사용한다.
 * 다른 BC가 Kafka로 전환 완료되면 Spring Event 발행을 점진적으로 제거할 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final Map<Class<?>, String> TOPIC_MAP = Map.ofEntries(
            // Payment 도메인
            Map.entry(PaymentSuccessEvent.class, PaymentSuccessEvent.TOPIC),
            Map.entry(PaymentFailedEvent.class, PaymentFailedEvent.TOPIC),
            Map.entry(PaymentCompensationFailedEvent.class, PaymentCompensationFailedEvent.TOPIC),
            Map.entry(RefundCompletedEvent.class, RefundCompletedEvent.TOPIC),
            Map.entry(RefundFailedEvent.class, RefundFailedEvent.TOPIC),
            // Deposit 도메인
            Map.entry(DepositUsedEvent.class, DepositUsedEvent.TOPIC),
            Map.entry(DepositDeductionFailedEvent.class, DepositDeductionFailedEvent.TOPIC),
            Map.entry(DepositRefundedEvent.class, DepositRefundedEvent.TOPIC),
            Map.entry(DepositChargeSucceededEvent.class, DepositChargeSucceededEvent.TOPIC),
            Map.entry(DepositChargeFailedEvent.class, DepositChargeFailedEvent.TOPIC)
    );

    /**
     * 이벤트를 Spring Event + Kafka로 동시 발행한다.
     *
     * <p>
     * Spring Event는 즉시 발행(다른 BC의 @TransactionalEventListener 하위 호환).
     * Kafka는 트랜잭션 커밋 후 발행(데이터 정합성 보장).
     */
    public void publish(Object event) {
        // 1. Spring Event 발행 (다른 BC의 @TransactionalEventListener용 - 하위 호환)
        applicationEventPublisher.publishEvent(event);

        // 2. Kafka 발행 (Payment/Deposit 도메인의 @KafkaListener용)
        String topic = resolveTopic(event);
        if (topic == null) {
            return;
        }

        String key = resolveKey(event);

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendToKafka(topic, key, event);
                        }
                    });
        } else {
            sendToKafka(topic, key, event);
        }
    }

    /**
     * 트랜잭션 완료(커밋 또는 롤백) 후 이벤트를 발행한다.
     *
     * <p>
     * 롤백 시에도 Kafka 발행이 필요한 실패/보상 이벤트에 사용한다.
     * 예: 예치금 차감 실패 시 트랜잭션을 롤백하면서 DepositDeductionFailedEvent를 발행하여
     * 결제 보상 트랜잭션을 트리거해야 하는 경우.
     */
    public void publishAfterCompletion(Object event) {
        // 1. Spring Event 발행 (다른 BC의 @TransactionalEventListener용 - 하위 호환)
        applicationEventPublisher.publishEvent(event);

        // 2. Kafka 발행
        String topic = resolveTopic(event);
        if (topic == null) {
            return;
        }

        String key = resolveKey(event);

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            sendToKafka(topic, key, event);
                        }
                    });
        } else {
            sendToKafka(topic, key, event);
        }
    }

    private void sendToKafka(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka 발행 실패] topic={}, key={}, event={}", topic, key,
                                event.getClass().getSimpleName(), ex);
                    } else {
                        log.debug("[Kafka 발행 성공] topic={}, key={}, partition={}, offset={}",
                                topic, key,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    private String resolveTopic(Object event) {
        return TOPIC_MAP.get(event.getClass());
    }

    /**
     * 이벤트에서 파티션 키(orderUuid 또는 settlementUuid)를 추출한다.
     * 동일한 주문/정산의 이벤트가 같은 파티션에 할당되어 순서가 보장된다.
     */
    private String resolveKey(Object event) {
        return switch (event) {
            case PaymentSuccessEvent e -> e.orderUuid().toString();
            case PaymentFailedEvent e -> e.orderUuid().toString();
            case PaymentCompensationFailedEvent e -> e.orderUuid().toString();
            case RefundCompletedEvent e -> e.orderUuid().toString();
            case RefundFailedEvent e -> e.orderUuid().toString();
            case DepositUsedEvent e -> e.orderUuid().toString();
            case DepositDeductionFailedEvent e -> e.orderUuid().toString();
            case DepositRefundedEvent e -> e.orderUuid().toString();
            case DepositChargeSucceededEvent e -> e.settlementUuid().toString();
            case DepositChargeFailedEvent e -> e.settlementUuid().toString();
            default -> null;
        };
    }
}
