package dukku.common.shared.product.out;

import dukku.common.shared.product.dto.cart.CartInternalResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class CartApiClient {

    private final RestClient restClient;

    public CartApiClient(@Value("${custom.global.internalBackUrl}") String internalBackUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/carts")
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
