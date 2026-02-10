package dukku.semicolon.boundedContext.payment.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.payment.event.PaymentCompensationFailedEvent;
import dukku.common.shared.payment.event.PaymentFailedEvent;
import dukku.common.shared.payment.type.PaymentFailureCode;
import dukku.common.shared.payment.type.PaymentFailureStage;
import dukku.common.shared.payment.type.PaymentHistoryType;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.semicolon.boundedContext.payment.entity.Payment;
import dukku.semicolon.boundedContext.payment.out.TossPaymentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 결제 보상(Compensating) 트랜잭션 UseCase.
 * <p>
 * 예치금 차감 실패 등 후행 단계에서 결제 일관성이 깨질 때 PG 취소를 수행하고
 * Payment 상태/이력을 실패로 정리한 뒤 실패 이벤트를 발행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompensatePaymentUseCase {

    private final PaymentSupport support;
    private final TossPaymentClient tossPaymentClient;
    private final EventPublisher eventPublisher;

    /**
     * 보상 트랜잭션 실행 (PG 취소 + 상태 정리 + 실패 이벤트 발행)
     *
     * @param orderUuid 주문 UUID
     * @param reason    취소/보상 사유
     */
    @Transactional
    public void execute(UUID orderUuid, String reason) {
        Payment payment = support.findPaymentsByOrderUuid(orderUuid).stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.DONE)
                .findFirst()
                .orElse(null);

        if (payment == null) {
            log.warn("[보상 트랜잭션 없음] 완료된 결제가 없습니다: orderUuid={}, reason={}", orderUuid, reason);
            return;
        }

        // 멱등성 검증 - 이미 보상 처리된 결제인지 확인 (PAYMENT_FAILED 이력 존재 여부)
        if (support.hasHistoryType(payment.getId(), PaymentHistoryType.PAYMENT_FAILED)) {
            log.info("[보상 멱등성] 이미 보상 처리된 결제입니다. paymentUuid={}, orderUuid={}",
                    payment.getUuid(), orderUuid);
            return;
        }

        log.info("[보상 트랜잭션 시작] paymentUuid={}, orderUuid={}, reason={}", payment.getUuid(), orderUuid, reason);

        PaymentStatus originStatus = payment.getPaymentStatus();
        Long originPg = payment.getAmountPg();
        Long originDeposit = payment.getPaymentDeposit();
        String failureReason = buildFailureReason(PaymentFailureCode.DEPOSIT_DEDUCTION_FAILED, reason);

        try {
            // PG 취소로 외부 결제 확정 되돌림
            cancelPg(payment, reason);
            // 보상 실패로 결제 상태/금액 정리
            payment.compensateAfterDepositFailure();
            // 상태/이력 저장
            support.savePayment(payment);
            support.createHistory(payment, PaymentHistoryType.PAYMENT_FAILED, originStatus, originPg, originDeposit);
            log.info("[보상 트랜잭션 완료] PG 취소 및 상태 정리 성공: paymentUuid={}", payment.getUuid());
        } catch (Exception e) {
            PaymentFailureCode failureCode = resolveCompensationFailureCode(e); // 보상 실패 코드 매핑
            // PG 취소/상태 정리 실패 별도 이벤트 발행 (운영 추적)
            log.error("[CRITICAL][보상 트랜잭션 실패] PG 취소/상태 정리 중 예외: paymentUuid={}, error={}", payment.getUuid(),
                    e.getMessage(), e);
            payment.rollbackFailedStatus();
            support.savePayment(payment);
            support.createHistory(payment, PaymentHistoryType.PAYMENT_ROLLBACK_FAILED, originStatus, originPg,
                    originDeposit);
            eventPublisher.publish(new PaymentCompensationFailedEvent(
                    orderUuid,
                    payment.getUuid(),
                    payment.getUserUuid(),
                    failureCode,
                    true,
                    buildFailureReason(failureCode, e.getMessage()),
                    LocalDateTime.now()));
            // throw 제거: @Transactional 커밋 보장 → 실패 상태 영속화 + AFTER_COMMIT 이벤트 발행
        } finally {
            // 보상 경로 진입 시 주문 롤백 트리거로 실패 이벤트 발행
            eventPublisher.publish(new PaymentFailedEvent(
                    orderUuid,
                    payment.getUuid(),
                    payment.getUserUuid(),
                    PaymentFailureStage.DEPOSIT_DEDUCTION,
                    PaymentFailureCode.DEPOSIT_DEDUCTION_FAILED,
                    false,
                    failureReason,
                    LocalDateTime.now()));
        }
    }

    private void cancelPg(Payment payment, String reason) {
        if (payment.getAmountPg() == null || payment.getAmountPg() <= 0) {
            return;
        }

        Map<String, Object> cancelBody = new HashMap<>();
        cancelBody.put("cancelReason", "COMPENSATION: " + reason);
        cancelBody.put("cancelAmount", payment.getAmountPg());

        Map<String, Object> response;
        try {
            response = tossPaymentClient.cancel(payment.getPgPaymentKey(), cancelBody);
        } catch (RuntimeException e) {
            throw new RuntimeException("TOSS_CANCEL_EXCEPTION: " + e.getMessage(), e);
        }
        int statusCode = ((Number) response.getOrDefault("statusCode", 200)).intValue();

        if (HttpStatus.valueOf(statusCode).isError()) {
            log.error("[PG 취소 실패] status={}, body={}, paymentUuid={}", statusCode, response, payment.getUuid());
            throw new RuntimeException("TOSS_CANCEL_FAILED: " + response.get("message"));
        }
    }

    private PaymentFailureCode resolveCompensationFailureCode(Exception e) {
        String message = e.getMessage();
        if (message != null) {
            if (message.startsWith("TOSS_CANCEL_FAILED")) {
                return PaymentFailureCode.PG_CANCEL_FAILED;
            }
            if (message.startsWith("TOSS_CANCEL_EXCEPTION")) {
                return PaymentFailureCode.PG_CANCEL_EXCEPTION;
            }
        }
        return PaymentFailureCode.STATE_PERSIST_FAILED;
    }

    private String buildFailureReason(PaymentFailureCode code, String detail) {
        if (detail == null || detail.isBlank()) {
            return code.name();
        }
        return code.name() + ": " + detail;
    }
}
