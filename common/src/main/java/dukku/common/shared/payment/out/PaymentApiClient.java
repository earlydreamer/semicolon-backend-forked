package dukku.common.shared.payment.out;

import dukku.common.shared.payment.dto.PaymentInternalResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class PaymentApiClient {

    private final RestClient restClient;

    public PaymentApiClient(@Value("${custom.client.payment.url:${custom.global.internalBackUrl}}") String internalBackUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/internal/payments")
                .build();
    }

    /**
     * 결제 UUID로 내부 결제 정보 조회
     */
    public PaymentInternalResponse getPaymentByUuid(UUID paymentUuid) {
        return restClient.get()
                .uri("/{paymentUuid}", paymentUuid)
                .retrieve()
                .body(PaymentInternalResponse.class);
    }

    /**
     * 주문 UUID로 내부 결제 정보 조회
     */
    public PaymentInternalResponse getPaymentByOrderUuid(UUID orderUuid) {
        return restClient.get()
                .uri("/orders/{orderUuid}", orderUuid)
                .retrieve()
                .body(PaymentInternalResponse.class);
    }
}
