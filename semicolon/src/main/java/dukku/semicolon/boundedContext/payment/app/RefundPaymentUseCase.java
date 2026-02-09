package dukku.semicolon.boundedContext.payment.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.semicolon.boundedContext.payment.entity.Payment;
import dukku.semicolon.boundedContext.payment.entity.Refund;
import dukku.common.shared.payment.type.PaymentHistoryType;
import dukku.common.shared.payment.type.PaymentFailureCode;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.semicolon.shared.payment.dto.PaymentRefundRequest;
import dukku.semicolon.shared.payment.dto.PaymentRefundResponse;
import dukku.common.shared.payment.event.RefundCompletedEvent;
import dukku.common.shared.payment.event.RefundFailedEvent;
import dukku.semicolon.shared.payment.exception.InvalidRefundAmountException;
import dukku.semicolon.shared.payment.exception.PaymentNotRefundableException;
import dukku.semicolon.boundedContext.payment.out.TossPaymentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import dukku.semicolon.boundedContext.deposit.app.IncreaseDepositUseCase;
import dukku.semicolon.boundedContext.deposit.entity.enums.DepositHistoryType;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 환불 처리 UseCase
 * 
 * <p>
 * 결제 완료된 건에 대해 사용자 요청 또는 시스템 이벤트에 의해 환불을 수행한다.
 * 외부 자산(PG) 취소 성공 후 내부 데이터(예치금/상태) 업데이트를 진행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundPaymentUseCase {

    private final PaymentSupport support;
    private final EventPublisher eventPublisher;
    private final TossPaymentClient tossClient;
    private final IncreaseDepositUseCase increaseDepositUseCase;

    /**
     * 환불 요청 처리
     *
     * @param request        환불 요청 정보
     * @param idempotencyKey 멱등성 키
     * @return 환불 결과
     */
    @Transactional
    public PaymentRefundResponse execute(PaymentRefundRequest request, String idempotencyKey) {
        // 1. 멱등성 검증 - 동일 idempotencyKey로 이미 처리된 환불이 있는지 확인
        Optional<Refund> existingRefund = support.findRefundByIdempotencyKey(idempotencyKey);
        if (existingRefund.isPresent()) {
            Refund refund = existingRefund.get();
            Payment payment = refund.getPayment();
            log.info("[환불 멱등성] 이미 처리된 환불 요청입니다. idempotencyKey={}, refundUuid={}",
                    idempotencyKey, refund.getUuid());

            // 기존 환불 결과 반환 (PG 환불액 = 총 환불액 - 예치금 환불액)
            Long pgRefundAmount = refund.getRefundAmountTotal() - refund.getRefundDepositTotal();
            return refund.toPaymentRefundResponse(pgRefundAmount, payment.getTossOrderId());
        }

        // 2. 결제 조회
        Payment payment = support.findPaymentByUuid(request.getPaymentId());

        // 3. 환불 가능 상태 검증
        validateRefundable(payment);

        // 4. 환불 금액 검증
        validateRefundAmount(payment, request.getRefundAmount());

        // 5. 상태 변경 전 값 저장 (이력용)
        PaymentStatus originStatus = payment.getPaymentStatus();
        Long originAmountPg = payment.getAmountPg();
        Long originDeposit = payment.getPaymentDeposit();

        // 6. 환불 금액 배분 산정
        // 정책에 따라 예치금 복구액과 PG 취소액을 계산 (엔티티 도메인 로직 활용)
        Payment.RefundAllocation allocation = payment.calculateRefundAllocation(request.getRefundAmount());

        // 7. PG 취소 수행 (외부 시스템 확정 먼저)
        // PG 확정 후 내부 금액 반영
        if (allocation.pgRefundAmount() > 0) {
            Map<String, Object> cancelBody = new HashMap<>();
            cancelBody.put("cancelReason", request.getReason());
            cancelBody.put("cancelAmount", allocation.pgRefundAmount());

            // PG 취소 호출 (5xx → @Retryable 재시도 → 소진 시 RuntimeException)
            Map<String, Object> response;
            try {
                response = tossClient.cancel(payment.getPgPaymentKey(), cancelBody);
            } catch (RuntimeException e) {
                log.error("[CRITICAL][취소 오류] PG 취소 중 예외 발생: {}. 관리자 확인 필요! paymentUuid={}",
                        e.getMessage(), payment.getUuid());
                handleFailure(payment, originStatus, originAmountPg, originDeposit,
                        "PG_CANCEL_EXCEPTION: " + e.getMessage());
                publishRefundFailed(payment, allocation, request,
                        PaymentFailureCode.REFUND_PG_CANCEL_EXCEPTION, true,
                        buildFailureReason(PaymentFailureCode.REFUND_PG_CANCEL_EXCEPTION, e.getMessage()));
                return PaymentRefundResponse.builder()
                        .success(false).code("PG_CANCEL_EXCEPTION")
                        .message("PG 취소 중 예외 발생: " + e.getMessage()).build();
            }

            // PG 취소 응답 상태 확인 (여기 도달 시 2xx or 4xx만 가능)
            int statusCode = ((Number) response.getOrDefault("statusCode", 200)).intValue();
            if (HttpStatus.valueOf(statusCode).isError()) {
                log.error("[CRITICAL][취소 오류] PG 취소 실패: status={}, body={}. 관리자 확인 필요! paymentUuid={}",
                        statusCode, response, payment.getUuid());
                handleFailure(payment, originStatus, originAmountPg, originDeposit,
                        "PG_CANCEL_FAILED: " + response.get("message"));
                publishRefundFailed(payment, allocation, request,
                        PaymentFailureCode.REFUND_PG_CANCEL_FAILED, false,
                        buildFailureReason(PaymentFailureCode.REFUND_PG_CANCEL_FAILED,
                                String.valueOf(response.get("message"))));
                return PaymentRefundResponse.builder()
                        .success(false).code("PG_CANCEL_FAILED")
                        .message("PG 취소 실패: " + response.get("message")).build();
            }
        }

        // 8. 예치금 복구 처리 (PG 성공 후)
        // PG 취소 성공 또는 예치금 단독 환불 시 실행
        if (allocation.depositRefundAmount() > 0) {
            increaseDepositUseCase.increase(
                    payment.getUserUuid(),
                    allocation.depositRefundAmount(),
                    DepositHistoryType.REFUND,
                    null);
        }

        // 9. Refund 엔티티 및 이력 생성 (idempotencyKey 포함)
        // 환불 스냅샷 영속화
        // 환불 스냅샷 생성
        Refund refund = payment.createRefund(request.getRefundAmount(), allocation.depositRefundAmount(), idempotencyKey);
        // 환불 완료 상태로 마킹
        refund.complete();
        // 환불 이력 저장
        support.saveRefund(refund);

        // 10. 결제 상태 및 잔액 업데이트
        // 환불 결과를 결제 상태/금액에 반영
        payment.partialCancel(request.getRefundAmount(), allocation.pgRefundAmount(), allocation.depositRefundAmount());
        // 결제 상태 저장
        support.savePayment(payment);

        // 11. 이력 생성
        // 결제 상태 히스토리 생성
        PaymentHistoryType historyType = (payment.getPaymentStatus() == PaymentStatus.CANCELED)
                ? PaymentHistoryType.FULL_REFUND_SUCCESS
                : PaymentHistoryType.PARTIAL_REFUND_SUCCESS;
        support.createHistory(payment, historyType, originStatus, originAmountPg, originDeposit);

        // 12. 이벤트 발행
        // 후속 처리 리스너용 이벤트 발행 (주문 취소 확정 등 환불 완료 후속 작업)
        eventPublisher.publish(new RefundCompletedEvent(
                refund.getUuid(),
                payment.getUuid(),
                payment.getOrderUuid(),
                request.getRefundAmount(),
                allocation.depositRefundAmount(),
                payment.getUserUuid(),
                refund.getCreatedAt()));

        return refund.toPaymentRefundResponse(allocation.pgRefundAmount(), payment.getTossOrderId());
    }

    // 환불 가능 상태 체크
    private void validateRefundable(Payment payment) {
        PaymentStatus status = payment.getPaymentStatus();
        // DONE, PARTIAL_CANCELED 상태만 환불 가능
        if (status != PaymentStatus.DONE && status != PaymentStatus.PARTIAL_CANCELED) {
            throw new PaymentNotRefundableException();
        }
    }

    // 환불 가능 금액 체크
    private void validateRefundAmount(Payment payment, Long refundAmount) {
        // 환불 가능 금액 = 현재 결제 금액 - 이미 환불된 금액
        Long refundableAmount = payment.getAmount() - payment.getRefundTotal();
        if (refundAmount > refundableAmount) {
            throw new InvalidRefundAmountException("환불 가능 금액 초과: " + refundableAmount);
        }
    }

    // 환불 실패 처리
    private void handleFailure(Payment payment, PaymentStatus originStatus, Long originAmountPg, Long originDeposit,
            String reason) {
        payment.rollbackFailedStatus();
        support.savePayment(payment);
        support.createHistory(payment, PaymentHistoryType.PAYMENT_ROLLBACK_FAILED, originStatus, originAmountPg,
                originDeposit);
    }

    private void publishRefundFailed(Payment payment, Payment.RefundAllocation allocation, PaymentRefundRequest request,
            PaymentFailureCode failureCode, boolean retryable, String reason) {
        eventPublisher.publish(new RefundFailedEvent(
                payment.getOrderUuid(),
                payment.getUuid(),
                payment.getUserUuid(),
                request.getRefundAmount(),
                allocation.pgRefundAmount(),
                allocation.depositRefundAmount(),
                failureCode,
                retryable,
                reason,
                LocalDateTime.now()));
    }

    private String buildFailureReason(PaymentFailureCode code, String detail) {
        if (detail == null || detail.isBlank()) {
            return code.name();
        }
        return code.name() + ": " + detail;
    }

}
