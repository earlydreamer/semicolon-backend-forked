package dukku.order.boundedContext.order.out;

import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * 주문 JPA 리포지토리.
 */
public interface OrderRepository extends JpaRepository<Order, Integer>, CustomOrderRepository {

    /**
     * 주문 UUID로 단건 조회한다.
     */
    Optional<Order> findByUuid(UUID orderUuid);

    /**
     * 주문 UUID로 주문 + 주문아이템을 함께 조회한다.
     */
    @EntityGraph(attributePaths = {"orderItems"})
    @Query("SELECT o FROM Order o WHERE o.uuid = :uuid")
    Optional<Order> findByUuidWithItems(@Param("uuid") UUID uuid);

    /**
     * 사용자 UUID + 주문 상태 조건으로 페이지 조회한다.
     */
    Page<Order> findByUserUuidAndStatus(
            UUID userUuid,
            OrderStatus status,
            Pageable pageable
    );
}
