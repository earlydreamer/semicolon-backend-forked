package dukku.common.shared.order.dto;

import dukku.common.shared.order.type.OrderStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자 주문 목록 조회 조건 DTO
 */
public record AdminOrderSearchCondition(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime startDate, // 조회 시작 시각(포함)

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime endDate, // 조회 종료 시각(포함)

        OrderStatus status, // 주문 상태 필터

        UUID userUuid, // 특정 구매자 UUID 필터

        String orderUuid // 주문 UUID(문자열) 필터
) {
}
