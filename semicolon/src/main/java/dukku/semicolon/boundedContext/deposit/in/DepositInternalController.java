package dukku.semicolon.boundedContext.deposit.in;

import dukku.semicolon.boundedContext.deposit.app.DepositFacade;
import dukku.semicolon.shared.deposit.dto.DepositChargeForSettlementRequest;
import dukku.semicolon.shared.deposit.dto.DepositChargeForSettlementResponse;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 예치금 Internal API 컨트롤러
 *
 * <p>
 * 내부 서비스(정산 배치 등)에서 호출하는 예치금 충전 API를 제공한다.
 * settlementUuid를 멱등키로 활용하여 중복 요청을 방지한다.
 *
 * <p>
 * <b>보안:</b> 이 API는 내부 서비스 간 통신 전용이며, 외부에 노출되지 않아야 한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/internal/deposits")
@RequiredArgsConstructor
@Hidden // Swagger 문서에서 숨김
public class DepositInternalController {

    private final DepositFacade depositFacade;

    /**
     * 정산에 의한 예치금 충전 (Internal API)
     *
     * <p>
     * 정산 배치에서 판매자 예치금을 충전할 때 사용한다.
     * settlementUuid가 멱등키로 동작하여 동일 요청에 대해 중복 충전을 방지한다.
     *
     * @param userUuid 충전 대상 사용자(판매자) UUID
     * @param request  충전 요청 정보 (금액, 정산 UUID)
     * @return 충전 결과
     */
    @PostMapping("/{userUuid}/charge")
    public ResponseEntity<DepositChargeForSettlementResponse> chargeForSettlement(
            @PathVariable UUID userUuid,
            @RequestBody @Valid DepositChargeForSettlementRequest request) {

        log.info("[Internal API] 정산 예치금 충전 요청. userUuid={}, amount={}, settlementUuid={}",
                userUuid, request.getAmount(), request.getSettlementUuid());

        DepositChargeForSettlementResponse response = depositFacade.chargeDepositForSettlementApi(
                userUuid,
                request.getAmount(),
                request.getSettlementUuid());

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            // 실패 시에도 200 OK로 응답 (멱등성 - 이미 처리된 경우 포함)
            // 클라이언트는 success 필드로 성공 여부를 판단
            return ResponseEntity.ok(response);
        }
    }
}
