package dukku.common.shared.product.dto.cart;

import dukku.common.shared.product.type.SaleStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CartDto(
        int cartId,            // 장바구니 PK (삭제 시 사용)
        UUID productUuid,       // 상품 UUID (상세 페이지 이동용)
        String title,           // 상품 제목
        long price,             // 현재 상품 가격 (실시간 반영)
        SaleStatus saleStatus,  // 판매 상태 (ON_SALE, SOLD_OUT 등)
        String thumbnailUrl,    // 썸네일 이미지 URL
        LocalDateTime createdAt // 장바구니에 담은 날짜
) {
}