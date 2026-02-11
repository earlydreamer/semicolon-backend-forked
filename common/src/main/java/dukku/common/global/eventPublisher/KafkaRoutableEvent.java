package dukku.common.global.eventPublisher;

/**
 * Kafka로 라우팅 가능한 이벤트 마커 인터페이스
 *
 * <p>
 * 이 인터페이스를 구현한 이벤트만 {@link EventPublisher}에 의해 Kafka로 발행된다.
 * 각 이벤트가 자신의 토픽과 파티션 키를 직접 제공하므로,
 * EventPublisher에 중앙 매핑 테이블을 유지할 필요가 없다.
 */
public interface KafkaRoutableEvent {

    /** Kafka 토픽명 */
    String topic();
}
