package dukku.semicolon.shared.deposit.out.depositApiClient;

import dukku.semicolon.shared.deposit.dto.DepositAccountResponse;
import dukku.semicolon.shared.deposit.dto.DepositChargeForSettlementRequest;
import dukku.semicolon.shared.deposit.dto.DepositChargeForSettlementResponse;
import dukku.semicolon.shared.deposit.exception.DepositNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class DepositApiClient {

    // TODO: admin/internal API가 혼재되어 있어 클라이언트를 분리했지만,
    // 추후 API 구조 재정리 시 통합 여부 재검토 필요
    private final RestClient adminClient;
    private final RestClient internalClient;

    public DepositApiClient(@Value("${custom.global.internalBackUrl}") String internalBackUrl) {
        this.adminClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/admin/deposits")
                .build();
        this.internalClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/internal/deposits")
                .build();
    }

    /**
     * 판매자 예치금 계좌 UUID 조회 (관리자/내부 용도)
     */
    public UUID getDepositUuid(UUID userUuid) {
        DepositAccountResponse response = adminClient.get()
                .uri("/{userUuid}/account", userUuid)
                .retrieve()
                .body(DepositAccountResponse.class);

        if (response != null && response.getData() != null) {
            return response.getData().getDepositUuid();
        }
        throw new DepositNotFoundException("예치금 계좌 정보를 찾을 수 없습니다.");
    }

    /**
     * 정산 예치금 충전 Internal API 호출
     */
    public DepositChargeForSettlementResponse chargeDepositForSettlement(
            UUID userUuid,
            Long amount,
            UUID settlementUuid
    ) {
        DepositChargeForSettlementRequest request = DepositChargeForSettlementRequest.builder()
                .amount(amount)
                .settlementUuid(settlementUuid)
                .build();

        return internalClient.post()
                .uri("/{userUuid}/charge", userUuid)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(DepositChargeForSettlementResponse.class);
    }
}