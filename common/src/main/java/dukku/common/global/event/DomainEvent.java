package dukku.common.global.event;

public interface DomainEvent {
    String getTopic();
    String getKey(); // Partition Key (e.g., aggregate ID)
}
