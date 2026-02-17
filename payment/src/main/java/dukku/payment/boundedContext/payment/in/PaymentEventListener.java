package dukku.payment.boundedContext.payment.in;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.deposit.event.DepositDeductionFailedEvent;
import dukku.common.shared.deposit.event.DepositRefundFailedEvent;
import dukku.common.shared.deposit.event.DepositRefundedEvent;
import dukku.common.shared.order.event.PaymentRollbackRequestEvent;
import dukku.common.shared.payment.event.RefundCompletedEvent;
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

import java.time.LocalDateTime;

/**
 * 결제 도메인 이벤트 리스너
 *
 * <p>주문과 예치금 도메인에서 전달된 이벤트를 수신해 결제 상태를 전이</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final PaymentFacade paymentFacade;
    private final PaymentSupport paymentSupport;
    private final EventPublisher eventPublisher;

    /**
     * 예치금 차감 실패 시 결제 보상 트랜잭션 처리
     */
    @KafkaListener(topics = "deposit.deduction-failed", groupId = "${spring.application.name}-group")
    public void handle(DepositDeductionFailedEvent event) {
        // 예치금 차감 실패를 결제 보상 흐름으로 전환
        paymentFacade.compensatePayment(event.orderUuid(), event.reason());
    }

    /**
     * 주문 처리 실패 시 결제 롤백 이벤트 처리
     */
    @KafkaListener(topics = "payment.rollback", groupId = "${spring.application.name}-group")
    public void handle(PaymentRollbackRequestEvent event) {
        // 주문 단위 롤백 요청을 받아 결제 보상 실행
        paymentFacade.compensatePayment(event.orderUuid(), event.reason());
    }

    /**
     * 예치금 환불 완료 이벤트 처리
     */
    @KafkaListener(topics = "deposit.refunded", groupId = "${spring.application.name}-group")
    public void handle(DepositRefundedEvent event) {
        paymentSupport.findRefundByUuid(event.refundUuid()).ifPresentOrElse(refund -> {
            // 이미 완료된 환불은 중복 이벤트로 간주
            if (refund.getRefundStatus() == RefundStatus.COMPLETED) {
                log.warn("[결제 Saga 중복] 이미 완료된 환불 이벤트는 무시 refundUuid={}", event.refundUuid());
                return;
            }

            refund.complete();
            paymentSupport.saveRefund(refund);

            Payment payment = refund.getPayment();
            eventPublisher.publish(new RefundCompletedEvent(
                    refund.getUuid(),
                    payment.getUuid(),
                    payment.getOrderUuid(),
                    refund.getRefundAmountTotal(),
                    refund.getRefundDepositTotal(),
                    payment.getUserUuid(),
                    LocalDateTime.now()));
        }, () -> log.warn("[결제 Saga 실패] deposit.refunded 조회 실패 refundUuid={}, paymentUuid={}",
                event.refundUuid(), event.paymentUuid()));
    }

    /**
     * 예치금 환불 실패 이벤트 처리
     */
    @KafkaListener(topics = "deposit.refund.failed", groupId = "${spring.application.name}-group")
    public void handle(DepositRefundFailedEvent event) {
        // 환불 엔티티를 취소 상태로 전환
        paymentSupport.findRefundByUuid(event.refundUuid()).ifPresentOrElse(refund -> {
            if (refund.getRefundStatus() == RefundStatus.COMPLETED) {
                log.warn("[결제 Saga 실패] 이미 완료된 환불 refundUuid={}", event.refundUuid());
                return;
            }

            refund.cancel();
            paymentSupport.saveRefund(refund);
        }, () -> log.warn("[결제 Saga 실패] deposit.refund.failed 조회 실패 refundUuid={}, paymentUuid={}",
                event.refundUuid(), event.paymentUuid()));

        paymentSupport.findPaymentByUuidOptional(event.paymentUuid()).ifPresentOrElse(payment -> {
            if (payment.getPaymentStatus() == PaymentStatus.ROLLBACK_FAILED) {
                return;
            }

            PaymentStatus originStatus = payment.getPaymentStatus();
            Long originAmountPg = payment.getAmountPg();
            Long originDeposit = payment.getPaymentDeposit();
            PaymentHistoryType historyType = resolveRefundFailureHistoryType(originStatus);

            payment.rollbackFailedStatus();
            paymentSupport.savePayment(payment);
            paymentSupport.createHistory(payment, historyType,
                    originStatus, originAmountPg, originDeposit);

            log.error("[결제 Saga 실패] deposit.refund.failed 처리 paymentUuid={}, reason={}",
                    event.paymentUuid(), event.reason());
        }, () -> log.warn("[결제 Saga 실패] payment 조회 실패 paymentUuid={}", event.paymentUuid()));
    }

    /**
     * 환불 실패 전 상태를 기준으로 이력 타입 계산
     *
     * @param originStatus 환불 실패 직전 결제 상태
     * @return 실패 이력 타입
     */
    private PaymentHistoryType resolveRefundFailureHistoryType(PaymentStatus originStatus) {
        if (originStatus == PaymentStatus.CANCELED) {
            return PaymentHistoryType.FULL_REFUND_FAILED;
        }
        if (originStatus == PaymentStatus.PARTIAL_CANCELED) {
            return PaymentHistoryType.PARTIAL_REFUND_FAILED;
        }
        return PaymentHistoryType.PAYMENT_ROLLBACK_FAILED;
    }
}