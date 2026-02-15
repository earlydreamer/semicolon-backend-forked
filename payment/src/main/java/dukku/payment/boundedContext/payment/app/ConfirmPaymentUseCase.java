package dukku.payment.boundedContext.payment.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.common.shared.payment.event.PaymentFailedEvent;
import dukku.common.shared.payment.event.PaymentSuccessEvent;
import dukku.common.shared.payment.type.PaymentFailureCode;
import dukku.common.shared.payment.type.PaymentFailureStage;
import dukku.common.shared.payment.type.PaymentHistoryType;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.common.shared.payment.dto.PaymentConfirmRequest;
import dukku.common.shared.payment.dto.PaymentConfirmResponse;
import dukku.common.shared.payment.exception.DuplicatePaymentKeyException;
import dukku.common.shared.payment.exception.PaymentNotPendingException;
import dukku.common.shared.payment.exception.TossAmountMismatchException;
import dukku.payment.boundedContext.payment.out.TossPaymentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 결제 승인 확정 UseCase
 *
 * <p>
 * 토스페이먼츠 인증 완료 후 백엔드에서 최종 승인 처리
 * {@link Payment#approve(String)} 호출하여 결제 확정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfirmPaymentUseCase {

    private final PaymentSupport support;
    private final TossPaymentClient tossClient;
    private final EventPublisher eventPublisher;

    /**
     * 결제 승인 확정 처리
     *
     * @param request        토스 인증 정보
     * @param idempotencyKey 멱등성 키
     * @return 결제 승인 결과
     */
    public PaymentConfirmResponse execute(PaymentConfirmRequest request, String idempotencyKey) {
        // 1. 결제 조회
        Payment payment = support.findPaymentByUuid(request.getPaymentUuid());

        // 2. 실패 복구용 상태/금액 스냅샷 저장
        PaymentStatus originStatus = payment.getPaymentStatus();
        Long originAmountPg = payment.getAmountPg();
        Long originDeposit = payment.getPaymentDeposit();

        try {
            // 승인 가능 상태 검증 (PENDING만 허용)
            validatePaymentStatus(payment);
            // PG 승인 금액 일치 검증
            validateAmount(payment, request.getToss().getAmount());
            // 동일 paymentKey 중복 승인 방지
            validateDuplicatePaymentKey(request.getToss().getPaymentKey());
        } catch (RuntimeException e) {
            PaymentFailureCode failureCode = resolveValidationFailureCode(e); // 검증 실패 코드 매핑
            handlePaymentFailure(
                    payment,
                    originStatus,
                    originAmountPg,
                    originDeposit,
                    PaymentFailureStage.VALIDATION,
                    failureCode,
                    false,
                    buildFailureReason(failureCode, e.getMessage()));
            return payment.toPaymentConfirmResponse(false, "검증 실패: " + e.getMessage());
        }

        // 6. 실제 토스페이먼츠 승인 요청 API 호출
        Map<String, Object> tossRequestBody = new HashMap<>();
        tossRequestBody.put("paymentKey", request.getToss().getPaymentKey());
        tossRequestBody.put("orderId", request.getToss().getOrderId());
        tossRequestBody.put("amount", request.getToss().getAmount());
        Map<String, Object> tossResponse;
        try {
            tossResponse = tossClient.confirm(tossRequestBody);
        } catch (RuntimeException e) {
            handlePaymentFailure(
                    payment,
                    originStatus,
                    originAmountPg,
                    originDeposit,
                    PaymentFailureStage.PG_CONFIRM,
                    PaymentFailureCode.PG_CONFIRM_EXCEPTION,
                    true,
                    buildFailureReason(PaymentFailureCode.PG_CONFIRM_EXCEPTION, e.getMessage()));
            return payment.toPaymentConfirmResponse(false, "PG 승인 중 예외 발생: " + e.getMessage());
        }

        int statusCode = ((Number) tossResponse.getOrDefault("statusCode", 200)).intValue();

        if (HttpStatus.valueOf(statusCode).isError()) {
            log.error("[Toss Confirm API Error] status={}, body={}", statusCode, tossResponse);
            boolean retryable = isRetryableStatus(statusCode);
            handlePaymentFailure(
                    payment,
                    originStatus,
                    originAmountPg,
                    originDeposit,
                    PaymentFailureStage.PG_CONFIRM,
                    PaymentFailureCode.PG_CONFIRM_FAILED,
                    retryable,
                    buildFailureReason(PaymentFailureCode.PG_CONFIRM_FAILED,
                            String.valueOf(tossResponse.get("message"))));
            return payment.toPaymentConfirmResponse(false, "PG 승인 실패: " + tossResponse.get("message"));
        }

        // 7. 시스템 내 결제 승인 (상태 변경 및 결제키 저장)
        // 승인 상태 반영 (PG paymentKey 저장)
        payment.approve(request.getToss().getPaymentKey());
        // 승인 상태 저장
        support.savePayment(payment);

        // 8. 결제 성공 이력 생성
        support.createHistory(payment, PaymentHistoryType.PAYMENT_SUCCESS, originStatus, originAmountPg, originDeposit);

        // 9. 예치금 차감
        // 예치금 사용 항목 추출 (차감 목록 구성)
        List<PaymentSuccessEvent.ItemDepositUsage> itemDepositUsages = payment.getItems().stream()
                .filter(item -> item.getPaymentDeposit() != null && item.getPaymentDeposit() > 0)
                .map(item -> new PaymentSuccessEvent.ItemDepositUsage(item.getOrderItemUuid(),
                        item.getPaymentDeposit()))
                .toList();

        // 10. 결제 성공 이벤트 발행 (주문 상태 변경 및 예치금 차감 트리거)
        eventPublisher.publish(new PaymentSuccessEvent(
                payment.getUuid(),
                payment.getOrderUuid(),
                payment.getAmount(),
                payment.getAmountPg(),
                payment.getPaymentDeposit(),
                payment.getUserUuid(),
                payment.getApprovedAt(),
                itemDepositUsages));

        return payment.toPaymentConfirmResponse(true, "결제가 승인되었습니다.");
    }

    private void validatePaymentStatus(Payment payment) {
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new PaymentNotPendingException();
        }
    }

    private void validateAmount(Payment payment, Long tossAmount) {
        if (!payment.getAmountPg().equals(tossAmount)) {
            throw new TossAmountMismatchException(payment.getAmountPg(), tossAmount);
        }
    }

    private void validateDuplicatePaymentKey(String paymentKey) {
        if (support.findPaymentByPgPaymentKey(paymentKey).isPresent()) {
            throw new DuplicatePaymentKeyException();
        }
    }

    private void handlePaymentFailure(Payment payment, PaymentStatus originStatus, Long originAmountPg,
            Long originDeposit, PaymentFailureStage failureStage, PaymentFailureCode failureCode, boolean retryable,
            String reason) {
        // PENDING 상태만 실패 처리 (중복 이벤트 방지)
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            return;
        }
        payment.fail();
        support.savePayment(payment);
        support.createHistory(payment, PaymentHistoryType.PAYMENT_FAILED, originStatus, originAmountPg, originDeposit);
        eventPublisher.publish(new PaymentFailedEvent(
                payment.getOrderUuid(),
                payment.getUuid(),
                payment.getUserUuid(),
                failureStage,
                failureCode,
                retryable,
                reason,
                LocalDateTime.now()));
    }

    private PaymentFailureCode resolveValidationFailureCode(RuntimeException e) {
        if (e instanceof PaymentNotPendingException) {
            return PaymentFailureCode.PAYMENT_STATUS_INVALID;
        }
        if (e instanceof TossAmountMismatchException) {
            return PaymentFailureCode.AMOUNT_MISMATCH;
        }
        if (e instanceof DuplicatePaymentKeyException) {
            return PaymentFailureCode.DUPLICATE_PAYMENT_KEY;
        }
        return PaymentFailureCode.UNKNOWN;
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode >= 500;
    }

    private String buildFailureReason(PaymentFailureCode code, String detail) {
        if (detail == null || detail.isBlank()) {
            return code.name();
        }
        return code.name() + ": " + detail;
    }
}
