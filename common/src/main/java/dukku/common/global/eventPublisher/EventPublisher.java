package dukku.common.global.eventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 이벤트 발행기 (Spring Event + Kafka 동시 발행)
 *
 * <p>
 * Phase A: Spring Event(하위 호환) + Kafka(Payment/Deposit 도메인용) 동시 발행.
 * DB 트랜잭션 커밋 후 Kafka 발행을 보장하기 위해 {@code TransactionSynchronization.afterCommit()}을 사용한다.
 * 다른 BC가 Kafka로 전환 완료되면 Spring Event 발행을 점진적으로 제거할 수 있다.
 *
 * <p>
 * {@link KafkaRoutableEvent}를 구현한 이벤트만 Kafka로 발행된다.
 * 각 이벤트가 자신의 토픽과 파티션 키를 직접 제공하므로 중앙 매핑 테이블이 필요 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 이벤트를 Spring Event + Kafka로 동시 발행한다.
     *
     * <p>
     * Spring Event는 즉시 발행(다른 BC의 @TransactionalEventListener 하위 호환).
     * Kafka는 트랜잭션 커밋 후 발행(데이터 정합성 보장).
     */
    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);

        if (!(event instanceof KafkaRoutableEvent routable)) {
            return;
        }

        String topic = routable.topic();

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendToKafka(topic, event);
                        }
                    });
        } else {
            sendToKafka(topic, event);
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
        applicationEventPublisher.publishEvent(event);

        if (!(event instanceof KafkaRoutableEvent routable)) {
            return;
        }

        String topic = routable.topic();

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            sendToKafka(topic, event);
                        }
                    });
        } else {
            sendToKafka(topic, event);
        }
    }

    private void sendToKafka(String topic, Object event) {
        kafkaTemplate.send(topic, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka 발행 실패] topic={}, event={}", topic,
                                event.getClass().getSimpleName(), ex);
                    } else {
                        log.debug("[Kafka 발행 성공] topic={}, partition={}, offset={}",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
