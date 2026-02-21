package dukku.common.shared.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class OrderCreateRequest {
    @NotBlank
    private String address;
    @Size(max = 50)
    @NotBlank
    private String recipient;
    @Size(max = 50)
    @NotBlank
    private String contactNumber;
    @NotEmpty
    @Valid
    private List<OrderItemCreateRequest> items;

    @Getter
    @NoArgsConstructor
    public static class OrderItemCreateRequest {
        @NotNull
        private UUID productUuid; // 상품 UUID

        @NotNull
        private Integer productId; // 상품 PK (결제 요청 스냅샷용)

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

