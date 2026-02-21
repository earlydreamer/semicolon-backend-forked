package dukku.common.shared.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 상품 운송장 정보 요청 DTO
 */
@Getter
@NoArgsConstructor
public class DeliveryInfoRequest {
    @Size(max = 50)
    @NotBlank
    private String carrierName; // 택배사 명

    @Size(max = 20)
    @NotBlank
    private String carrierCode; // 택배사 코드

    @Size(max = 50)
    @NotBlank
    private String trackingNumber; // 운송장 번호
}
