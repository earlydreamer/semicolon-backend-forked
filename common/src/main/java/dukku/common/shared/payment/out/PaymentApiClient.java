package dukku.common.shared.payment.out;

import dukku.common.global.auth.InternalServiceTokenResolver;
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
        RestClient.RequestHeadersSpec<?> requestSpec = restClient.get()
                .uri("/{paymentUuid}", paymentUuid);

        String internalToken = InternalServiceTokenResolver.resolve();
        if (internalToken != null) {
            requestSpec = requestSpec.header(InternalServiceTokenResolver.HEADER_NAME, internalToken);
        }

        return requestSpec.retrieve()
                .body(PaymentInternalResponse.class);
    }

    /**
     * 주문 UUID로 내부 결제 정보 조회
     */
    public PaymentInternalResponse getPaymentByOrderUuid(UUID orderUuid) {
        RestClient.RequestHeadersSpec<?> requestSpec = restClient.get()
                .uri("/orders/{orderUuid}", orderUuid);

        String internalToken = InternalServiceTokenResolver.resolve();
        if (internalToken != null) {
            requestSpec = requestSpec.header(InternalServiceTokenResolver.HEADER_NAME, internalToken);
        }

        return requestSpec.retrieve()
                .body(PaymentInternalResponse.class);
    }
}
