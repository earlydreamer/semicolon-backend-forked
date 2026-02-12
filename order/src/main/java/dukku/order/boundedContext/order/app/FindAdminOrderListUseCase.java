package dukku.order.boundedContext.order.app;

import dukku.common.global.UserUtil;
import dukku.common.global.exception.ForbiddenException;
import dukku.common.shared.order.dto.AdminOrderSearchCondition;
import dukku.common.shared.order.dto.OrderListResponse;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindAdminOrderListUseCase {
    private final OrderRepository orderRepository;

    public Page<OrderListResponse> execute(AdminOrderSearchCondition condition, Pageable pageable) {
        if (!UserUtil.isAdmin()) {
            throw new ForbiddenException("관리자만 조회할 수 있습니다.");
        }

        Page<Order> orders = orderRepository.searchForAdmin(condition, pageable);

        return orders.map(Order::fromOrderListResponse);
    }
}