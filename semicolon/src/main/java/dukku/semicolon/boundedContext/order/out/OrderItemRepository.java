package dukku.semicolon.boundedContext.order.out;

import dukku.common.shared.order.type.OrderItemStatus;
import dukku.semicolon.boundedContext.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    Optional<OrderItem> findByUuid(UUID orderItemUuid);

    List<OrderItem> findAllByStatusAndDeliveryDateBefore(OrderItemStatus status, LocalDateTime dateTime);
  
    List<OrderItem> findAllByStatusAndConfirmedAtBetween(
            OrderItemStatus status,
            LocalDateTime start,
            LocalDateTime end
    );
}
