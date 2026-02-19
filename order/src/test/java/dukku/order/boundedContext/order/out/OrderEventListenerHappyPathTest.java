package dukku.order.boundedContext.order.out;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.order.type.OrderStatus;
import dukku.common.shared.payment.event.RefundCompletedEvent;
import dukku.order.boundedContext.order.app.UpdateOrderStatusUseCase;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.in.OrderEventListener;
import dukku.order.boundedContext.order.out.ProcessedRefundEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * payment.refund-completed 이벤트 수신 시 주문 상태 반영 동작 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:order_listener_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.elasticsearch.uris=http://localhost:9200",
        "jwt.access.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktYWNjZXNzLTAxMjM=",
        "jwt.refresh.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktcmVmcmVzaC0wMTI=",
        "crypto.key=dGhpcy1rZXktaXMtdGVzdC1rZXktY3J5cHRvLTAxMjM="
})
class OrderEventListenerHappyPathTest {

    @Autowired
    private OrderEventListener listener;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProcessedRefundEventRepository processedRefundEventRepository;

    @MockitoBean
    private UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @MockitoBean
    private EventPublisher eventPublisher;

    @BeforeEach
    void cleanUp() {
        processedRefundEventRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("전체 환불 이벤트를 수신하면 환불액이 누적되고 상태가 CANCELED로 바뀐다")
    void appliesFullRefundValuesFromEventToOrder() {
        // given: PAID 주문과 전체 환불 이벤트 준비
        Order order = orderRepository.save(newOrder(15000));

        RefundCompletedEvent event = new RefundCompletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                order.getUuid(),
                15000L,
                0L,
                UUID.randomUUID(),
                LocalDateTime.now());

        // when: payment.refund-completed 이벤트 처리
        listener.handle(event);

        // then: 환불 금액 누적과 상태 변경 확인
        Order updated = orderRepository.findByUuid(order.getUuid()).orElseThrow();
        assertThat(updated.getRefundedAmount()).isEqualTo(15000);
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("부분 환불 이벤트를 수신하면 환불액이 누적되고 상태가 PARTIAL_REFUNDED가 된다")
    void appliesPartialRefundValuesFromEventToOrder() {
        // given: PAID 주문과 부분 환불 이벤트 준비
        Order order = orderRepository.save(newOrder(20000));

        RefundCompletedEvent event = new RefundCompletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                order.getUuid(),
                5000L,
                0L,
                UUID.randomUUID(),
                LocalDateTime.now());

        // when: payment.refund-completed 이벤트 처리
        listener.handle(event);

        // then: 부분 환불 금액과 상태 반영 확인
        Order updated = orderRepository.findByUuid(order.getUuid()).orElseThrow();
        assertThat(updated.getRefundedAmount()).isEqualTo(5000);
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PARTIAL_REFUNDED);
    }

    @Test
    @DisplayName("동일 refundUuid를 여러 번 받으면 최초 한 번만 반영된다")
    void duplicateRefundCompletedEventIsIgnored() {
        // given: 동일 refundUuid를 가진 중복 이벤트 준비
        Order order = orderRepository.save(newOrder(10000));
        UUID refundUuid = UUID.randomUUID();

        RefundCompletedEvent duplicated = new RefundCompletedEvent(
                refundUuid,
                UUID.randomUUID(),
                order.getUuid(),
                5000L,
                0L,
                UUID.randomUUID(),
                LocalDateTime.now());

        // when: 동일 이벤트를 두 번 처리
        listener.handle(duplicated);
        listener.handle(duplicated);

        // then: 두 번째는 무시되어 5000만 반영
        Order updated = orderRepository.findByUuid(order.getUuid()).orElseThrow();
        assertThat(updated.getRefundedAmount()).isEqualTo(5000);
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PARTIAL_REFUNDED);
    }

    @Test
    @DisplayName("서로 다른 refundUuid는 각각 별도로 반영된다")
    void appliesDifferentRefundUuidsSeparately() {
        // given: 서로 다른 refundUuid를 가진 이벤트 두 개 준비
        Order order = orderRepository.save(newOrder(10000));

        RefundCompletedEvent first = new RefundCompletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                order.getUuid(),
                5000L,
                0L,
                UUID.randomUUID(),
                LocalDateTime.now());
        RefundCompletedEvent second = new RefundCompletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                order.getUuid(),
                5000L,
                0L,
                UUID.randomUUID(),
                LocalDateTime.now());

        // when: 각 이벤트를 순차 처리
        listener.handle(first);
        listener.handle(second);

        // then: 두 이벤트 금액이 모두 누적되는지 확인
        Order updated = orderRepository.findByUuid(order.getUuid()).orElseThrow();
        assertThat(updated.getRefundedAmount()).isEqualTo(10000);
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    private Order newOrder(int totalAmount) {
        return Order.builder()
                .userUuid(UUID.randomUUID())
                .totalAmount(totalAmount)
                .address("seoul")
                .recipient("tester")
                .contactNumber("010-1111-2222")
                .refundedAmount(0)
                .status(OrderStatus.PAID)
                .build();
    }
}