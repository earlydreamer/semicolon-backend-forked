package dukku.common.shared.product.out;

import dukku.common.global.auth.InternalServiceTokenResolver;
import dukku.common.shared.product.dto.product.ProductReserveRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
public class ProductApiClient {

    private final RestClient restClient;

    public ProductApiClient(@Value("${custom.client.product.url:${custom.global.internalBackUrl}}") String internalBackUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/products")
                .build();
    }

    /**
     * 상품들을 예약 상태(RESERVED)로 변경하고 주문 UUID를 기록합니다.
     */
    public void reserveProducts(UUID orderUuid, List<UUID> productUuids) {
        ProductReserveRequest request = new ProductReserveRequest(orderUuid, productUuids);

        RestClient.RequestBodySpec requestSpec = restClient.post()
                .uri("/internal/reserve") // Controller에 정의한 경로
                .contentType(MediaType.APPLICATION_JSON);

        String internalToken = InternalServiceTokenResolver.resolve();
        if (internalToken != null) {
            requestSpec = requestSpec.header(InternalServiceTokenResolver.HEADER_NAME, internalToken);
        }

        requestSpec.body(request)
                .retrieve()
                .toBodilessEntity(); // 응답 본문이 없을 때 사용 (void)
    }
}
