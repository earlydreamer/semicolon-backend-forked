package dukku.common.global.eventPublisher;

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
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

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
