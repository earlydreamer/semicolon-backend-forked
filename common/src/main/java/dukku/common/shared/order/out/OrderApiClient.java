package dukku.common.shared.order.out;

import dukku.common.shared.order.dto.ConfirmedOrderItemResponse;
import dukku.common.shared.order.dto.OrderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    // 결제 도메인에서 주문 만료 검증에 사용할 주문 상세 정보를 조회한다.
    public OrderResponse findOrderByUuid(UUID orderUuid) {
        return restClient.get()
                .uri("/{orderUuid}/detail", orderUuid)
                .retrieve()
                .body(OrderResponse.class);
    }
}
