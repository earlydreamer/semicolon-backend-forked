package dukku.semicolon.boundedContext.deposit.in;

import dukku.semicolon.boundedContext.deposit.app.DepositFacade;
import dukku.semicolon.shared.deposit.dto.DepositBalanceResponse;
import dukku.semicolon.shared.deposit.dto.DepositChargeForSettlementRequest;
import dukku.semicolon.shared.deposit.dto.DepositChargeForSettlementResponse;
import dukku.semicolon.shared.deposit.dto.DepositDto;
import dukku.semicolon.shared.deposit.type.DepositChargeResultCode;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 예치금 Internal API 컨트롤러
 *
 * <p>
 * 외부 서비스의 정산 배치 과정에서 호출되는 예치금 충전 API를 제공합니다.
 * settlementUuid를 멱등키로 사용하여 중복 요청을 방지합니다.
 *
 * <p>
 * <b>보안:</b> 이 API는 내부 서비스 간 통신 전용이며 외부 노출하지 않습니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/internal/deposits")
@RequiredArgsConstructor
@Hidden
public class DepositInternalController {

    private final DepositFacade depositFacade;

    /**
     * 사용자 예치금 잔액 조회 (Internal API)
     */
    @GetMapping("/{userUuid}/balance")
    public ResponseEntity<DepositBalanceResponse> getUserBalanceInternal(@PathVariable UUID userUuid) {
        DepositDto deposit = depositFacade.findDeposit(userUuid);

        DepositBalanceResponse response = DepositBalanceResponse.builder()
                .success(true)
                .code("DEPOSIT_BALANCE_RETRIEVED")
                .message("예치금 잔액을 조회했습니다.")
                .data(DepositBalanceResponse.DepositBalanceData.builder()
                        .balance(deposit.getBalance())
                        .updatedAt(deposit.getUpdatedAt() != null ? deposit.getUpdatedAt() :
                                deposit.getCreatedAt())
                        .build())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 정산을 위한 예치금 충전 (Internal API)
     *
     * <p>
     * 정산 배치에서 판매자의 예치금을 충전하기 위해 사용합니다.
     * settlementUuid를 멱등키로 사용하여 동일 요청의 중복 충전을 방지합니다.
     *
     * @param userUuid 충전 대상 사용자 UUID
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
        }

        if (DepositChargeResultCode.INVALID_AMOUNT.getCode().equals(response.getCode())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
