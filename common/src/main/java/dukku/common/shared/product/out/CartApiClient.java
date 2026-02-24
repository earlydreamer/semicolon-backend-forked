package dukku.common.shared.product.out;

import dukku.common.shared.product.dto.cart.CartInternalResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.UUID;

@Component
public class CartApiClient {

    private final RestClient restClient;

    public CartApiClient(@Value("${custom.client.cart.url:${custom.global.internalBackUrl}}") String internalBackUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/carts")
                .requestFactory(factory)
                .build();
    }

    /**
     * 특정 사용자의 장바구니 목록을 조회합니다. (AI 추천용)
     */
    public CartInternalResponse findCartByUserUuid(UUID userUuid) {
        return restClient.get()
                .uri("/internal/{userUuid}", userUuid)
                .retrieve()
                .body(CartInternalResponse.class);
    }
}
