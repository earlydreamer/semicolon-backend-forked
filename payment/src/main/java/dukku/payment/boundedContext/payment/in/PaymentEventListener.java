package dukku.payment.boundedContext.payment.in;

import dukku.common.shared.deposit.event.DepositDeductionFailedEvent;
import dukku.common.shared.deposit.event.DepositRefundFailedEvent;
import dukku.common.shared.deposit.event.DepositRefundedEvent;
import dukku.common.shared.order.event.PaymentRollbackRequestEvent;
import dukku.common.shared.payment.dto.PaymentRefundRequest;
import dukku.common.shared.payment.dto.PaymentRefundResponse;
import dukku.common.shared.payment.type.PaymentHistoryType;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.common.shared.payment.type.RefundStatus;
import dukku.payment.boundedContext.payment.app.PaymentFacade;
import dukku.payment.boundedContext.payment.app.PaymentSupport;
import dukku.payment.boundedContext.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 결제 도메인 이벤트 리스너 (Inbound Adapter)
 *
 * <p>
 * 외부 시스템(주문, 예치금 등)에서 발생한 이벤트를 수신하여 결제 도메인 로직을 구동한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final PaymentFacade paymentFacade;
    private final PaymentSupport paymentSupport;

    /**
     * 예치금 차감 실패 시 보상 트랜잭션(결제 취소) 처리
     *
     * <p>
     * DepositDeductionFailedEvent 수신 시 이미 승인된 PG 결제를 취소하여 데이터 일관성을 유지함.
     */
    @KafkaListener(topics = "deposit.deduction-failed", groupId = "${spring.application.name}-group")
    public void handle(DepositDeductionFailedEvent event) {
        log.warn("[결제 보상 트랜잭션 시작] 예치금 차감 실패 감지: orderUuid={}, reason={}",
                event.orderUuid(), event.reason());

        paymentFacade.compensatePayment(event.orderUuid(), event.reason());
    }

    /**
     * 주문 처리 실패 시 결제 롤백(자동 환불) 처리
     */
    @KafkaListener(topics = "payment.rollback", groupId = "${spring.application.name}-group")
    public void handle(PaymentRollbackRequestEvent event) {
        log.info("[결제 롤백] 주문 처리 실패로 인한 자동 환불 시작. orderUuid={}, reason={}",
                event.orderUuid(), event.reason());

        // 해당 주문에 대한 완료된 결제 조회
        List<Payment> payments = paymentSupport.findPaymentsByOrderUuid(event.orderUuid());

        for (Payment payment : payments) {
            try {
                PaymentRefundRequest refundRequest = PaymentRefundRequest.builder()
                        .paymentId(payment.getUuid())
                        .refundAmount(payment.getAmount() - payment.getRefundTotal())
                        .reason(event.reason())
                        .build();

                // Idempotency Key는 내부 롤백이므로 Prefix 사용
                PaymentRefundResponse response = paymentFacade.refundPayment(refundRequest,
                        "rollback-" + payment.getUuid());
                if (response.isSuccess()) {
                    log.info("[결제 롤백] 환불 성공. paymentUuid={}", payment.getUuid());
                } else {
                    log.error("[결제 롤백] PG 환불 실패. 수동 조치 필요. paymentUuid={}, code={}, message={}",
                            payment.getUuid(), response.getCode(), response.getMessage());
                }
            } catch (Exception e) {
                // 자동 롤백 프로세스 중 발생하는 모든 예외를 잡아서 로그를 남겨야 함.
                // 여기서 예외를 놓치면 결제는 성공했는데 주문은 실패한 상태로 남을 수 있음 (데이터 불일치)
                // 상위로 전파하면 다른 결제 건의 롤백이 중단될 수 있으므로, 여기서 예외를 먹고 CRITICAL 로그를 남겨 수동 조치 유도
                log.error("[결제 롤백] 환불 실패! 수동 조치 필요. paymentUuid={}, error={}",
                        payment.getUuid(), e.getMessage());
            }
        }
    }

    /**
     * 예치금 환불(복구) 성공 시 환불 Saga 완료 처리.
     */
    @KafkaListener(topics = "deposit.refunded", groupId = "${spring.application.name}-group")
    public void handle(DepositRefundedEvent event) {
        paymentSupport.findRefundByUuid(event.refundId()).ifPresentOrElse(refund -> {
            if (refund.getRefundStatus() == RefundStatus.COMPLETED) {
                return;
            }
            refund.complete();
            paymentSupport.saveRefund(refund);
            log.info("[환불 Saga 완료] refundUuid={}, paymentUuid={}", event.refundId(), event.paymentUuid());
        }, () -> log.warn("[환불 Saga] refundUuid를 찾을 수 없습니다. refundUuid={}, paymentUuid={}",
                event.refundId(), event.paymentUuid()));
    }

    /**
     * 예치금 환불(복구) 실패 시 결제/환불 상태를 장애 상태로 전환.
     */
    @KafkaListener(topics = "deposit.refund.failed", groupId = "${spring.application.name}-group")
    public void handle(DepositRefundFailedEvent event) {
        paymentSupport.findRefundByUuid(event.refundId()).ifPresentOrElse(refund -> {
            if (refund.getRefundStatus() == RefundStatus.COMPLETED) {
                log.warn("[환불 Saga 실패 무시] 이미 완료된 환불 이벤트입니다. refundUuid={}", event.refundId());
                return;
            }
            refund.cancel();
            paymentSupport.saveRefund(refund);
        }, () -> log.warn("[환불 Saga 실패] refundUuid를 찾을 수 없습니다. refundUuid={}, paymentUuid={}",
                event.refundId(), event.paymentUuid()));

        paymentSupport.findPaymentByUuidOptional(event.paymentUuid()).ifPresentOrElse(payment -> {
            if (payment.getPaymentStatus() == PaymentStatus.ROLLBACK_FAILED) {
                return;
            }

            PaymentStatus originStatus = payment.getPaymentStatus();
            Long originAmountPg = payment.getAmountPg();
            Long originDeposit = payment.getPaymentDeposit();

            payment.rollbackFailedStatus();
            paymentSupport.savePayment(payment);
            paymentSupport.createHistory(payment, PaymentHistoryType.PAYMENT_ROLLBACK_FAILED,
                    originStatus, originAmountPg, originDeposit);

            log.error("[환불 Saga 실패] 예치금 환불 실패로 롤백 실패 상태 전환. paymentUuid={}, reason={}",
                    event.paymentUuid(), event.reason());
        }, () -> log.warn("[환불 Saga 실패] paymentUuid를 찾을 수 없습니다. paymentUuid={}", event.paymentUuid()));
    }
}
