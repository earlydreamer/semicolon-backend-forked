package dukku.order.boundedContext.order.out;

import dukku.common.shared.order.type.OrderItemStatus;
import dukku.order.boundedContext.order.entity.OrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 주문아이템 JPA 리포지토리.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    /**
     * 주문아이템 UUID로 단건 조회한다.
     */
    Optional<OrderItem> findByUuid(UUID orderItemUuid);

    /**
     * 특정 상태 + 기준 시각 이전 배송일인 주문아이템을 조회한다.
     */
    List<OrderItem> findAllByStatusAndDeliveryDateBefore(OrderItemStatus status, LocalDateTime dateTime);

    /**
     * 구매확정 상태 + 기간 조건으로 주문아이템을 주문과 함께 조회한다.
     */
    @EntityGraph(attributePaths = {"order"})
    List<OrderItem> findAllByStatusAndConfirmedAtBetween(
            OrderItemStatus status,
            LocalDateTime start,
            LocalDateTime end
    );
}
