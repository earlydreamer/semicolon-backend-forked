package dukku.common.shared.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 주문 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
public class OrderCreateRequest {
    @NotBlank
    private String address; // 배송지 주소

    @Size(max = 50)
    @NotBlank
    private String recipient; // 수령인 이름

    @Size(max = 50)
    @NotBlank
    private String contactNumber; // 수령인 연락처

    @NotEmpty
    @Valid
    private List<OrderItemCreateRequest> items; // 주문 상품 목록

    /**
     * 주문 상품 생성 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    public static class OrderItemCreateRequest {
        @NotNull
        private UUID productUuid; // 상품 UUID

        @NotNull
        private UUID sellerUuid; // 판매자 UUID

        @Size(max = 100)
        @NotBlank
        private String productName; // 상품명

        @Positive
        private int productPrice; // 상품 가격

        private String imageUrl; // 상품 썸네일 이미지 URL
    }
}

