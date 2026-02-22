package dukku.common.shared.order.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

/**
 * 반품 신청 생성 요청 DTO
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReturnRequestCreateDto {
    private String reason; // 반품 사유
    private List<UUID> orderItemUuids; // 반품 대상 주문 상품 UUID 목록
}
