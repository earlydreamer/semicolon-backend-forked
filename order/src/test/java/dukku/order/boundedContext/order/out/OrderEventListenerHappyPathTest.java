package dukku.order.boundedContext.order.out;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.order.type.OrderStatus;
import dukku.common.shared.payment.event.RefundCompletedEvent;
import dukku.order.boundedContext.order.app.UpdateOrderStatusUseCase;
import dukku.order.boundedContext.order.entity.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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

    // payment.success/payment.failed 경로는 본 테스트 범위가 아니므로 mock 처리
    @MockitoBean
    private UpdateOrderStatusUseCase updateOrderStatusUseCase;

    // Kafka publish 의존성을 제거하기 위해 mock 처리
    @MockitoBean
    private EventPublisher eventPublisher;

    @BeforeEach
    void cleanUp() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("전체 환불 이벤트를 수신하면 주문의 환불금액이 누적되고 상태가 CANCELED가 된다")
    void appliesFullRefundValuesFromEventToOrder() {
        // given: PAID 주문이 저장되어 있고, 해당 주문으로 full refund 이벤트가 들어온다.
        Order order = Order.builder()
                .userUuid(UUID.randomUUID())
                .totalAmount(15000)
                .address("seoul")
                .recipient("tester")
                .contactNumber("010-1111-2222")
                .refundedAmount(0)
                .status(OrderStatus.PAID)
                .build();
        order = orderRepository.save(order);

        RefundCompletedEvent event = new RefundCompletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                order.getUuid(),
                15000L,
                0L,
                UUID.randomUUID(),
                LocalDateTime.now());

        // when: order BC 리스너가 payment.refund-completed 이벤트를 처리한다.
        listener.handle(event);

        // then: 실제 DB에 환불 금액이 누적되고 주문 상태가 CANCELED로 반영된다.
        Order updated = orderRepository.findByUuid(order.getUuid()).orElseThrow();
        assertThat(updated.getRefundedAmount()).isEqualTo(15000);
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("부분 환불 이벤트를 수신하면 주문의 환불금액이 누적되고 상태가 PARTIAL_REFUNDED가 된다")
    void appliesPartialRefundValuesFromEventToOrder() {
        // given: PAID 주문이 저장되어 있고, 부분 환불 이벤트가 들어온다.
        Order order = Order.builder()
                .userUuid(UUID.randomUUID())
                .totalAmount(20000)
                .address("seoul")
                .recipient("tester")
                .contactNumber("010-1111-2222")
                .refundedAmount(0)
                .status(OrderStatus.PAID)
                .build();
        order = orderRepository.save(order);

        RefundCompletedEvent event = new RefundCompletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                order.getUuid(),
                5000L,
                0L,
                UUID.randomUUID(),
                LocalDateTime.now());

        // when: order BC 리스너가 payment.refund-completed 이벤트를 처리한다.
        listener.handle(event);

        // then: 실제 DB에 부분 환불 금액이 반영되고 주문 상태가 PARTIAL_REFUNDED로 전이된다.
        Order updated = orderRepository.findByUuid(order.getUuid()).orElseThrow();
        assertThat(updated.getRefundedAmount()).isEqualTo(5000);
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PARTIAL_REFUNDED);
    }
}
