package dukku.common.shared.order.out;

import dukku.common.shared.order.dto.ConfirmedOrderItemResponse;
import dukku.common.shared.order.dto.OrderListResponse;
import dukku.common.shared.order.dto.OrderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class OrderApiClient {

    private final RestClient restClient;

    public OrderApiClient(@Value("${custom.client.order.url:${custom.global.internalBackUrl}}") String internalBackUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/internal/orders")
                .requestFactory(factory)
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

    // 특정 사용자의 주문 이력 조회 (AI 추천용)
    public List<OrderListResponse> findOrdersByUserUuid(UUID userUuid, String status, int limit) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{userUuid}")
                        .queryParam("status", status)
                        .queryParam("limit", limit)
                        .build(userUuid)
                )
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
