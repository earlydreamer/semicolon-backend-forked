package dukku.order.boundedContext.order.out.impl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import dukku.common.shared.order.dto.AdminOrderSearchCondition;
import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.out.CustomOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static dukku.order.boundedContext.order.entity.QOrder.order;
import static dukku.order.boundedContext.order.entity.QOrderItem.orderItem;

/**
 * 주문 커스텀 조회 구현체.
 * 1:N(주문-주문아이템) 페이징에서 N+1/중복을 피하기 위해
 * "ID 페이지 조회 -> fetch join 재조회" 2단계 방식을 사용한다.
 */
@RequiredArgsConstructor
public class CustomOrderRepositoryImpl implements CustomOrderRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 내 주문 목록을 페이지 조회한다.
     */
    @Override
    public Page<Order> findAllMyOrders(UUID userUuid, Pageable pageable) {
        // 1) 페이지 범위의 주문 ID만 먼저 조회한다.
        List<Integer> pageOrderIds = queryFactory
                .select(order.id)
                .from(order)
                .where(order.userUuid.eq(userUuid))
                .orderBy(resolveOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 주문 아이템을 fetch join으로 한 번에 로딩한다.
        List<Order> content = fetchOrdersWithItems(pageOrderIds);

        // 3) total count를 계산해 Page로 반환한다.
        JPAQuery<Long> countQuery = queryFactory
                .select(order.count())
                .from(order)
                .where(order.userUuid.eq(userUuid));

        return PageableExecutionUtils.getPage(
                content,
                pageable,
                () -> Optional.ofNullable(countQuery.fetchOne()).orElse(0L)
        );
    }

    /**
     * 관리자 조건으로 주문 목록을 페이지 조회한다.
     */
    @Override
    public Page<Order> searchForAdmin(AdminOrderSearchCondition condition, Pageable pageable) {
        // 1) 검색 조건 + 페이지 범위로 주문 ID를 먼저 조회한다.
        List<Integer> pageOrderIds = queryFactory
                .select(order.id)
                .from(order)
                .where(
                        userUuidEq(condition.userUuid()),
                        orderUuidContains(condition.orderUuid()),
                        dateBetween(condition.startDate(), condition.endDate()),
                        statusEq(condition.status())
                )
                .orderBy(resolveOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 주문 아이템을 fetch join으로 한 번에 로딩한다.
        List<Order> content = fetchOrdersWithItems(pageOrderIds);

        // 3) total count를 계산해 Page로 반환한다.
        JPAQuery<Long> countQuery = queryFactory
                .select(order.count())
                .from(order)
                .where(
                        userUuidEq(condition.userUuid()),
                        orderUuidContains(condition.orderUuid()),
                        dateBetween(condition.startDate(), condition.endDate()),
                        statusEq(condition.status())
                );

        return PageableExecutionUtils.getPage(
                content,
                pageable,
                () -> Optional.ofNullable(countQuery.fetchOne()).orElse(0L)
        );
    }

    /**
     * 특정 사용자 + 주문 상태 기준 최근 주문을 조회한다.
     */
    @Override
    public List<Order> findRecentByUserUuidAndStatusWithItems(UUID userUuid, OrderStatus status, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        // 1) 최근 주문 ID를 먼저 조회한다.
        List<Integer> orderIds = queryFactory
                .select(order.id)
                .from(order)
                .where(
                        order.userUuid.eq(userUuid),
                        order.status.eq(status)
                )
                .orderBy(order.createdAt.desc(), order.id.desc())
                .limit(limit)
                .fetch();

        // 2) 주문 + 주문아이템을 함께 조회한다.
        return fetchOrdersWithItems(orderIds);
    }

    /**
     * 주문 ID 목록으로 주문 + 주문아이템을 fetch join 조회한다.
     * ID 목록 순서대로 결과를 재정렬해 페이지 순서를 보존한다.
     */
    private List<Order> fetchOrdersWithItems(List<Integer> orderIds) {
        if (orderIds.isEmpty()) {
            return List.of();
        }

        List<Order> fetchedOrders = queryFactory
                .selectDistinct(order)
                .from(order)
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .where(order.id.in(orderIds))
                .fetch();

        Map<Integer, Integer> orderPositionById = new HashMap<>();
        for (int i = 0; i < orderIds.size(); i++) {
            orderPositionById.put(orderIds.get(i), i);
        }

        fetchedOrders.sort(Comparator.comparingInt(o -> orderPositionById.getOrDefault(o.getId(), Integer.MAX_VALUE)));
        return fetchedOrders;
    }

    /**
     * 조회 기간 조건.
     */
    private BooleanExpression dateBetween(LocalDateTime start, LocalDateTime end) {
        return (start != null && end != null) ? order.createdAt.between(start, end) : null;
    }

    /**
     * 주문 상태 조건.
     */
    private BooleanExpression statusEq(OrderStatus status) {
        return status != null ? order.status.eq(status) : null;
    }

    /**
     * 사용자 UUID 조건.
     */
    private BooleanExpression userUuidEq(UUID userUuid) {
        return userUuid != null ? order.userUuid.eq(userUuid) : null;
    }

    /**
     * 주문 UUID 문자열 조건.
     * UUID 파싱 실패 시 결과 없음(false) 조건을 반환한다.
     */
    private BooleanExpression orderUuidContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        try {
            return order.uuid.eq(UUID.fromString(keyword));
        } catch (IllegalArgumentException e) {
            return Expressions.asBoolean(false).isTrue();
        }
    }

    /**
     * Pageable 정렬 정보를 Querydsl 정렬로 변환한다.
     * 지원하지 않는 필드는 무시하고, 안정성을 위해 id 보조 정렬을 항상 추가한다.
     */
    private OrderSpecifier<?>[] resolveOrderSpecifiers(Pageable pageable) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        for (Sort.Order sortOrder : pageable.getSort()) {
            OrderSpecifier<?> orderSpecifier = toOrderSpecifier(sortOrder);
            if (orderSpecifier != null) {
                orderSpecifiers.add(orderSpecifier);
            }
        }

        if (orderSpecifiers.isEmpty()) {
            orderSpecifiers.add(order.createdAt.desc());
        }

        boolean hasIdSort = pageable.getSort().stream()
                .anyMatch(sortOrder -> "id".equals(sortOrder.getProperty()));

        if (!hasIdSort) {
            orderSpecifiers.add(order.id.desc());
        }

        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }

    /**
     * 단일 Sort.Order를 Querydsl OrderSpecifier로 변환한다.
     */
    private OrderSpecifier<?> toOrderSpecifier(Sort.Order sortOrder) {
        boolean ascending = sortOrder.isAscending();
        return switch (sortOrder.getProperty()) {
            case "id" -> ascending ? order.id.asc() : order.id.desc();
            case "uuid" -> ascending ? order.uuid.asc() : order.uuid.desc();
            case "createdAt" -> ascending ? order.createdAt.asc() : order.createdAt.desc();
            case "updatedAt" -> ascending ? order.updatedAt.asc() : order.updatedAt.desc();
            case "status" -> ascending ? order.status.asc() : order.status.desc();
            case "totalAmount" -> ascending ? order.totalAmount.asc() : order.totalAmount.desc();
            case "refundedAmount" -> ascending ? order.refundedAmount.asc() : order.refundedAmount.desc();
            case "userUuid" -> ascending ? order.userUuid.asc() : order.userUuid.desc();
            default -> null;
        };
    }
}
