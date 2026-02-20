package dukku.common.global.event;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface DomainEvent {
    /**
     * Kafka 전송용 메타데이터 (메시지 페이로드에서 제외)
     */
    @JsonIgnore
    String getTopic();

    @JsonIgnore
    String getKey(); // Partition Key (e.g., aggregate ID)
}
