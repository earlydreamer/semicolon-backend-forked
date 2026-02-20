package dukku.common.global.eventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import dukku.common.global.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(DomainEvent event) {
        boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
        boolean syncActive = TransactionSynchronizationManager.isSynchronizationActive();

        if (txActive && syncActive) {
            try {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        send(event);
                    }
                });
                return;
            } catch (IllegalStateException e) {
                log.debug("동기화 등록을 생략했습니다. 즉시 전송합니다. topic={}, reason={}",
                        event.getTopic(), e.getMessage());
            }
        }

        send(event);
    }

    /**
     * 트랜잭션 커밋 이후에만 이벤트를 발행하도록 보장하는 명시적 API
     * 현재 스레드에 활성 트랜잭션과 동기화가 존재하면, 커밋이 완료된 후에 이벤트를 전송합니다.
     * 트랜잭션이나 동기화가 없을 경우, 즉시 전송
     */
    public void publishAfterCommit(DomainEvent event) {
        boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
        boolean syncActive = TransactionSynchronizationManager.isSynchronizationActive();

        if (txActive && syncActive) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event);
                }
            });
        } else {
            // 트랜잭션이 활성화되어 있지 않으면 즉시 전송
            send(event);
        }
    }

    private void send(DomainEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            log.info("Kafka에 이벤트 발행: topic={}, key={}", event.getTopic(), event.getKey());
            kafkaTemplate.send(event.getTopic(), event.getKey(), eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("이벤트 발행 실패: {}", event, ex);
                        } else {
                            log.debug("이벤트 발행 성공: offset={}", result.getRecordMetadata().offset());
                        }
                    });
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("이벤트 직렬화 실패: {}", event, e);
        }
    }
}
