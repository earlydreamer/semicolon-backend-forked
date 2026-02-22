package dukku.common.shared.order.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 기간 내 구매 확정 주문 상품 조회 응답 DTO
 */
public record ConfirmedOrderItemResponse(
        UUID orderItemUuid,      // 주문 상품 UUID
        UUID orderUuid,          // 주문 UUID
        UUID buyerUuid,          // 구매자 UUID
        UUID sellerUuid,         // 판매자 UUID
        UUID productUuid,        // 상품 UUID
        String productName,      // 상품명
        int productPrice,       // 상품 가격
        LocalDateTime confirmedAt // 구매 확정 일시
) {}
