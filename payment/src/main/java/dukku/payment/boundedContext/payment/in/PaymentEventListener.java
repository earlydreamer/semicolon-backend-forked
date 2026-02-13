package dukku.payment.boundedContext.payment.in;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.deposit.event.DepositDeductionFailedEvent;
import dukku.common.shared.deposit.event.DepositRefundFailedEvent;
import dukku.common.shared.deposit.event.DepositRefundedEvent;
import dukku.common.shared.order.event.PaymentRollbackRequestEvent;
import dukku.common.shared.payment.dto.PaymentRefundRequest;
import dukku.common.shared.payment.dto.PaymentRefundResponse;
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
import java.util.List;

/**
 * 결제 도메인 이벤트 리스너 (Inbound Adapter)
 *
 * <p>
 * 외부 시스템(주문, 예치금 등)에서 발생한 이벤트를 수신해 결제 도메인 로직 구동
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final PaymentFacade paymentFacade;
    private final PaymentSupport paymentSupport;
    private final EventPublisher eventPublisher;

    /**
     * 예치금 차감 실패 시 보상 트랜잭션(결제 취소) 처리
     *
     * <p>
     * DepositDeductionFailedEvent 수신 시 이미 승인된 PG 결제를 취소해 데이터 일관성 유지
     */
    @KafkaListener(topics = "deposit.deduction-failed", groupId = "${spring.application.name}-group")
    public void handle(DepositDeductionFailedEvent event) {
        // 예치금 차감 실패는 주문 보상 결제 프로세스 트리거
        log.warn("[결제 보상] 예치금 차감 실패 수신: orderUuid={}, reason={}",
                event.orderUuid(), event.reason());

        paymentFacade.compensatePayment(event.orderUuid(), event.reason());
    }

    /**
     * 주문 처리 실패 시 결제 롤백(자동 환불) 처리
     */
    @KafkaListener(topics = "payment.rollback", groupId = "${spring.application.name}-group")
    public void handle(PaymentRollbackRequestEvent event) {
        // 주문 롤백 이벤트는 주문 단위의 모든 결제 건을 순차 환불 처리
        log.info("[결제 롤백] 주문 처리 실패로 인한 자동 환불 시작: orderUuid={}, reason={}",
                event.orderUuid(), event.reason());

        List<Payment> payments = paymentSupport.findPaymentsByOrderUuid(event.orderUuid());

        for (Payment payment : payments) {
            try {
                // 단건 환불을 위한 request 구성
                PaymentRefundRequest refundRequest = PaymentRefundRequest.builder()
                        .paymentId(payment.getUuid())
                        .orderUuid(event.orderUuid())
                        .refundAmount(payment.getAmount() - payment.getRefundTotal())
                        .reason(event.reason())
                        .build();

                // 부분 실패가 전체 루프를 막지 않도록 결제 건 단위로 try-catch 격리
                PaymentRefundResponse response = paymentFacade.refundPayment(refundRequest,
                        "rollback-" + payment.getUuid());
                if (response.isSuccess()) {
                    log.info("[결제 롤백] 환불 완료: paymentUuid={}", payment.getUuid());
                } else {
                    log.error("[결제 롤백] PG 환불 실패: paymentUuid={}, code={}, message={}",
                            payment.getUuid(), response.getCode(), response.getMessage());
                }
            } catch (Exception e) {
                // 자동 롤백 프로세스 중 발생하는 모든 예외를 잡아 로그를 남겨야 함
                // 여기서 예외를 놓치면 결제는 성공했는데 주문은 실패한 상태로 남을 수 있음 (데이터 불일치)
                // 상위로 전파하면 다른 결제 건의 롤백이 중단될 수 있으므로, 여기서 예외를 먹고 CRITICAL 로그를 남겨 수동 조치 유도
                log.error("[결제 롤백] 환불 처리 중 예외: paymentUuid={}, error={}",
                        payment.getUuid(), e.getMessage());
                // 예외는 로그 후 다음 결제 건 처리로 넘어가 전체 롤백 중단 방지
            }
        }
    }

    /**
     * 예치금 환불(복구) 성공 시 환불 Saga 완료 처리
     */
    @KafkaListener(topics = "deposit.refunded", groupId = "${spring.application.name}-group")
    public void handle(DepositRefundedEvent event) {
        paymentSupport.findRefundByUuid(event.refundId()).ifPresentOrElse(refund -> {
            // 이미 완료된 환불이면 중복 이벤트로 보고 무시
            if (refund.getRefundStatus() == RefundStatus.COMPLETED) {
                return;
            }

            refund.complete();
            paymentSupport.saveRefund(refund);
            eventPublisher.publish(new RefundCompletedEvent(
                    refund.getUuid(),
                    event.paymentUuid(),
                    event.orderUuid(),
                    refund.getRefundAmountTotal(),
                    refund.getRefundDepositTotal(),
                    event.userUuid(),
                    LocalDateTime.now()));
            log.info("[환불 Saga] refundUuid={}, paymentUuid={}", event.refundId(), event.paymentUuid());
        }, () -> log.warn("[환불 Saga] refund 이벤트 조회 실패: refundUuid={}, paymentUuid={}",
                event.refundId(), event.paymentUuid()));
    }

    /**
     * 예치금 환불(복구) 실패 시 결제/환불 상태를 장애 상태로 전환
     */
    @KafkaListener(topics = "deposit.refund.failed", groupId = "${spring.application.name}-group")
    public void handle(DepositRefundFailedEvent event) {
        // 환불 실패 시 refund 상태를 취소로 전환하고 주문 결제 상태도 장애로 반영
        paymentSupport.findRefundByUuid(event.refundId()).ifPresentOrElse(refund -> {
            if (refund.getRefundStatus() == RefundStatus.COMPLETED) {
                log.warn("[환불 Saga 실패] 이미 완료된 환불입니다. refundUuid={}", event.refundId());
                return;
            }
            refund.cancel();
            paymentSupport.saveRefund(refund);
        }, () -> log.warn("[환불 Saga 실패] refund 이벤트 조회 실패: refundUuid={}, paymentUuid={}",
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

            log.error("[환불 Saga 실패] deposit.refund 실패: paymentUuid={}, reason={}",
                    event.paymentUuid(), event.reason());
        }, () -> log.warn("[환불 Saga 실패] paymentUuid 조회 실패: paymentUuid={}", event.paymentUuid()));
    }
}
