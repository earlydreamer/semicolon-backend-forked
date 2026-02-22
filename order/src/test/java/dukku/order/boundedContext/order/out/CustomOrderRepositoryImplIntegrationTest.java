package dukku.order.boundedContext.order.out;

import dukku.common.shared.order.dto.AdminOrderSearchCondition;
import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.entity.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 주문 커스텀 리포지토리 조회 로직 통합 테스트.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:order_custom_repo_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
@Transactional
class CustomOrderRepositoryImplIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("내 주문 조회는 pageable 정렬(totalAmount ASC)을 반영한다")
    void 내주문조회_정렬반영() {
        // given: 동일 사용자의 여러 주문 데이터를 정렬 검증 가능하게 준비한다.
        UUID userUuid = UUID.randomUUID();
        saveOrder(userUuid, 30000, OrderStatus.PAID);
        saveOrder(userUuid, 10000, OrderStatus.PAID);
        saveOrder(userUuid, 20000, OrderStatus.PAID);

        // when: totalAmount 오름차순 정렬로 내 주문 조회를 실행한다.
        Page<Order> page = orderRepository.findAllMyOrders(
                userUuid,
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("totalAmount")))
        );

        // then: 조회 결과의 totalAmount가 오름차순으로 반환된다.
        List<Integer> totalAmounts = page.getContent().stream()
                .map(Order::getTotalAmount)
                .toList();
        assertThat(totalAmounts).containsExactly(10000, 20000, 30000);
    }

    @Test
    @DisplayName("관리자 주문 조회에서 잘못된 orderUuid 필터면 결과가 비어야 한다")
    void 관리자조회_잘못된주문UUID_결과없음() {
        // given: orderUuid 필터가 유효하지 않은 관리자 조회 조건을 준비한다.
        saveOrder(UUID.randomUUID(), 10000, OrderStatus.PAID);
        saveOrder(UUID.randomUUID(), 20000, OrderStatus.CANCELED);
        AdminOrderSearchCondition condition = new AdminOrderSearchCondition(
                null,
                null,
                null,
                null,
                "not-a-uuid"
        );

        // when: 관리자 주문 검색을 실행한다.
        Page<Order> page = orderRepository.searchForAdmin(
                condition,
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt")))
        );

        // then: 잘못된 UUID 필터로 인해 결과가 비어 있어야 한다.
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("지원하지 않는 정렬 필드는 기본 정렬로 대체된다")
    void 내주문조회_미지원정렬_기본정렬대체() {
        // given: 지원하지 않는 정렬 필드로 내 주문 조회 요청을 준비한다.
        UUID userUuid = UUID.randomUUID();
        saveOrder(userUuid, 10000, OrderStatus.PAID);
        saveOrder(userUuid, 20000, OrderStatus.PAID);

        // when: 미지원 정렬 필드로 내 주문 조회를 실행한다.
        Page<Order> page = orderRepository.findAllMyOrders(
                userUuid,
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("unsupportedField")))
        );

        // then: 조회는 성공하고 기본 정렬 규칙으로 결과가 반환된다.
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
    }

    private void saveOrder(UUID userUuid, int totalAmount, OrderStatus status) {
        Order order = Order.builder()
                .userUuid(userUuid)
                .totalAmount(totalAmount)
                .address("seoul")
                .recipient("tester")
                .contactNumber("010-1111-2222")
                .refundedAmount(0)
                .status(status)
                .build();
        orderRepository.save(order);
    }
}
