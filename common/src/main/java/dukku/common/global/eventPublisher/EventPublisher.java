package dukku.common.global.eventPublisher;

import dukku.common.global.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 도메인 이벤트 발행기
 * KafkaTemplate<String, String> 기반 이벤트 전송
 * 트랜잭션 활성 시 Commit 이후 전송되도록 동기화 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /**
     * 이벤트 발행
     * 트랜잭션 활성 상태 시 afterCommit 동기화를 통한 데이터 정합성 보장
     */
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
                log.debug("Synchronization registration skipped. send now. topic={}, reason={}",
                        event.getTopic(), e.getMessage());
            }
        }

        send(event);
    }

    /**
     * Kafka로 실제 메시지 전송
     * 프로젝트 표준인 객체(JSON 문자열) 기반 전송을 위한 objectMapper 직접 직렬화
     */
    private void send(DomainEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            log.info("Publishing event to Kafka: topic={}, key={}", event.getTopic(), event.getKey());
            kafkaTemplate.send(event.getTopic(), event.getKey(), eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event: {}", event, ex);
                        } else {
                            log.debug("Event published successfully: offset={}", result.getRecordMetadata().offset());
                        }
                    });
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Failed to serialize event: {}", event, e);
        }
    }
}
