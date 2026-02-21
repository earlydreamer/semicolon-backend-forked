package dukku.common.shared.order.dto;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReturnTrackingRegisterDto {
    private String carrierName;
    private String carrierCode;
    private String trackingNumber;
}
