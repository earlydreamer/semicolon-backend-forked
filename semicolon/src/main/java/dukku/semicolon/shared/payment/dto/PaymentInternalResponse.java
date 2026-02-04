package dukku.semicolon.shared.payment.dto;

import dukku.common.shared.payment.type.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Internal용 결제 응답 DTO (심플)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInternalResponse {
    private UUID paymentUuid;
    private UUID orderUuid;
    private PaymentStatus status;
    private Long totalAmount;
    private Long pgPayAmount;
    private Long depositUseAmount;
}
