package dukku.semicolon.boundedContext.payment.in;

import dukku.semicolon.boundedContext.payment.app.FindPaymentUseCase;
import dukku.semicolon.boundedContext.payment.entity.Payment;
import dukku.semicolon.shared.payment.dto.PaymentInternalResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Payment Internal API 컨트롤러
 *
 * <p>
 * 내부 서비스에서 결제 정보를 조회할 때 사용하는 API를 제공한다.
 *
 * <p>
 * <b>보안:</b> 이 API는 내부 서비스 간 통신 전용이며, 외부에 노출되지 않아야 한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/internal/payments")
@RequiredArgsConstructor
@Hidden // Swagger 문서에서 숨김
public class PaymentInternalController {

    private final FindPaymentUseCase findPayment;

    /**
     * UUID로 결제 정보 조회 (Internal API)
     *
     * @param paymentUuid 결제 UUID
     * @return 결제 정보
     */
    @GetMapping("/{paymentUuid}")
    public ResponseEntity<PaymentInternalResponse> getPaymentByUuid(
            @PathVariable UUID paymentUuid) {

        log.info("[Internal API] 결제 정보 조회 요청. paymentUuid={}", paymentUuid);

        Payment payment = findPayment.execute(paymentUuid);

        PaymentInternalResponse response = PaymentInternalResponse.builder()
                .paymentUuid(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .status(payment.getPaymentStatus())
                .totalAmount(payment.getAmount())
                .pgPayAmount(payment.getAmountPg())
                .depositUseAmount(payment.getPaymentDeposit())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 주문 UUID로 결제 정보 조회 (Internal API)
     *
     * @param orderUuid 주문 UUID
     * @return 결제 정보
     */
    @GetMapping("/orders/{orderUuid}")
    public ResponseEntity<PaymentInternalResponse> getPaymentByOrderUuid(
            @PathVariable UUID orderUuid) {

        log.info("[Internal API] 주문 결제 정보 조회 요청. orderUuid={}", orderUuid);

        Payment payment = findPayment.findByOrderUuid(orderUuid);

        PaymentInternalResponse response = PaymentInternalResponse.builder()
                .paymentUuid(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .status(payment.getPaymentStatus())
                .totalAmount(payment.getAmount())
                .pgPayAmount(payment.getAmountPg())
                .depositUseAmount(payment.getPaymentDeposit())
                .build();

        return ResponseEntity.ok(response);
    }

}
