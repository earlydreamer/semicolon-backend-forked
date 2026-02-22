package dukku.common.shared.order.dto;

import lombok.*;

/**
 * 반품 운송장 등록 요청 DTO
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReturnTrackingRegisterDto {
    private String carrierName; // 택배사 명
    private String carrierCode; // 택배사 코드
    private String trackingNumber; // 운송장 번호
}
