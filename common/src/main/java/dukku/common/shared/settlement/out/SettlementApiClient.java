package dukku.common.shared.settlement.out;

import dukku.common.global.auth.RequestAuthorizationHeaderResolver;
import dukku.common.shared.settlement.dto.SettlementDetailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;


@Component
public class SettlementApiClient {

    private final RestClient restClient;

    public SettlementApiClient(@Value("${custom.client.settlement.url:${custom.global.internalBackUrl}}") String internalBackUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/admin/settlements")
                .build();
    }

    public SettlementDetailResponse getSettlement(UUID settlementUuid) {
        RestClient.RequestHeadersSpec<?> requestSpec = restClient.get()
                .uri("/{settlementUuid}", settlementUuid);

        String authorization = RequestAuthorizationHeaderResolver.resolve();
        if (authorization != null) {
            requestSpec = requestSpec.header("Authorization", authorization);
        }

        return requestSpec.retrieve()
                .body(SettlementDetailResponse.class);
    }

    public SettlementDetailResponse[] getSettlementsBySeller(UUID sellerUuid) {
        RestClient.RequestHeadersSpec<?> requestSpec = restClient.get()
                .uri("?sellerUuid={sellerUuid}", sellerUuid);

        String authorization = RequestAuthorizationHeaderResolver.resolve();
        if (authorization != null) {
            requestSpec = requestSpec.header("Authorization", authorization);
        }

        return requestSpec.retrieve()
                .body(SettlementDetailResponse[].class);
    }
}
