package dukku.order.boundedContext.order.out;

import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Integer>, CustomOrderRepository {
    Optional<Order> findByUuid(UUID orderUuid);

    @Query("SELECT o FROM Order o JOIN FETCH o.orderItems WHERE o.uuid = :uuid")
    Optional<Order> findByUuidWithItems(@Param("uuid") UUID uuid);

    Page<Order> findByUserUuidAndStatus(
            UUID userUuid,
            OrderStatus status,
            Pageable pageable
    );
}
