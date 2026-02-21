package dukku.order.boundedContext.order.out;

import dukku.common.shared.order.dto.AdminOrderSearchCondition;
import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * 주문 조회 커스텀 리포지토리.
 */
public interface CustomOrderRepository {

    /**
     * 특정 사용자의 주문 목록을 페이지로 조회한다.
     */
    Page<Order> findAllMyOrders(UUID userUuid, Pageable pageable);

    /**
     * 관리자 조건으로 주문 목록을 페이지 조회한다.
     */
    Page<Order> searchForAdmin(AdminOrderSearchCondition condition, Pageable pageable);

    /**
     * 특정 사용자 + 주문 상태 기준 최근 주문을 주문아이템과 함께 조회한다.
     */
    List<Order> findRecentByUserUuidAndStatusWithItems(UUID userUuid, OrderStatus status, int limit);
}
