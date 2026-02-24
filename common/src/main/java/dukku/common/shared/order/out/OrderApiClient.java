package dukku.common.shared.order.out;

import dukku.common.shared.order.dto.ConfirmedOrderItemResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderApiClient {

    private final RestClient restClient;

    public OrderApiClient(@Value("${custom.client.order.url:${custom.global.internalBackUrl}}") String internalBackUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/internal/orders")
                .build();
    }

    // 구매 확정 주문 상품 기간 조회
    public List<ConfirmedOrderItemResponse> findConfirmedItems(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/confirmed")
                        .queryParam("startDateTime", startDateTime)
                        .queryParam("endDateTime", endDateTime)
                        .build()
                )
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}